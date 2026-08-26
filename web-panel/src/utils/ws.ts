/**
 * WebSocket 客户端封装
 * - 指数退避重连: 1s / 2s / 4s / 8s / 16s / 30s (封顶)
 * - 对外暴露 onopen / onmessage / onerror / onclose 回调
 * - 状态回调 onStateChange: 'connected' | 'reconnecting' | 'disconnected'
 */

export type WSState = 'connected' | 'reconnecting' | 'disconnected'
export type WSEventHandler = (event: Event) => void
export type WSMessageHandler = (data: unknown) => void
export type WSStateHandler = (state: WSState, latencyMs?: number) => void

export class WSClient {
  private url: string
  private protocols?: string | string[]
  private ws: WebSocket | null = null
  private retryCount = 0
  private maxRetryDelay = 30_000
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null
  private manualClose = false

  // Event hooks
  public onopen: WSEventHandler | null = null
  public onmessage: WSMessageHandler | null = null
  public onerror: WSEventHandler | null = null
  public onclose: WSEventHandler | null = null
  public onStateChange: WSStateHandler | null = null

  // Latency probe
  private lastPingAt = 0
  private latencyMs = 0
  private pingTimer: ReturnType<typeof setInterval> | null = null

  constructor(url: string, protocols?: string | string[]) {
    this.url = url
    this.protocols = protocols
  }

  public get currentState(): WSState {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) return 'connected'
    if (this.reconnectTimer) return 'reconnecting'
    return 'disconnected'
  }

  public get latency(): number {
    return this.latencyMs
  }

  public connect(): void {
    this.manualClose = false
    this.retryCount = 0
    this.doConnect()
  }

  public close(code = 1000, reason = 'manual_close'): void {
    this.manualClose = true
    this.clearPingTimer()
    this.clearReconnectTimer()
    if (this.ws) {
      try { this.ws.close(code, reason) } catch { /* noop */ }
      this.ws = null
    }
    this.notifyState('disconnected')
  }

  public send(data: string | ArrayBufferLike | Blob | ArrayBufferView): void {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      throw new Error('WebSocket is not connected')
    }
    this.ws.send(data)
  }

  /** 发送 JSON 数据 */
  public sendJSON<T = unknown>(payload: T): void {
    this.send(JSON.stringify(payload))
  }

  // ---------- internal ----------

  private doConnect(): void {
    this.clearReconnectTimer()

    if (!('WebSocket' in window)) {
      this.notifyState('disconnected')
      return
    }

    if (this.retryCount > 0) this.notifyState('reconnecting')

    try {
      this.ws = this.protocols
        ? new WebSocket(this.url, this.protocols)
        : new WebSocket(this.url)
    } catch (e) {
      this.scheduleReconnect()
      return
    }

    this.ws.onopen = (ev) => {
      this.retryCount = 0
      this.notifyState('connected')
      this.startPingTimer()
      this.onopen?.(ev)
    }

    this.ws.onmessage = (ev) => {
      // 处理心跳 PONG
      if (typeof ev.data === 'string' && ev.data.startsWith('__PONG__')) {
        if (this.lastPingAt > 0) {
          this.latencyMs = Date.now() - this.lastPingAt
          this.onStateChange?.('connected', this.latencyMs)
        }
        return
      }
      let parsed: unknown = ev.data
      if (typeof ev.data === 'string') {
        try { parsed = JSON.parse(ev.data) } catch { parsed = ev.data }
      }
      this.onmessage?.(parsed)
    }

    this.ws.onerror = (ev) => {
      this.onerror?.(ev)
    }

    this.ws.onclose = (ev) => {
      this.clearPingTimer()
      this.onclose?.(ev)
      if (!this.manualClose) {
        this.scheduleReconnect()
      } else {
        this.notifyState('disconnected')
      }
      this.ws = null
    }
  }

  private scheduleReconnect(): void {
    this.clearReconnectTimer()
    this.notifyState('reconnecting')
    this.retryCount += 1
    // 指数退避: 1, 2, 4, 8, 16, 30, 30...
    const delayMs = Math.min(this.maxRetryDelay, Math.pow(2, Math.min(this.retryCount, 4)) * 1000)
    this.reconnectTimer = setTimeout(() => this.doConnect(), delayMs)
  }

  private clearReconnectTimer(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
  }

  private startPingTimer(): void {
    this.clearPingTimer()
    this.pingTimer = setInterval(() => {
      if (this.ws && this.ws.readyState === WebSocket.OPEN) {
        this.lastPingAt = Date.now()
        try { this.ws.send('__PING__') } catch { /* noop */ }
      }
    }, 15_000)
  }

  private clearPingTimer(): void {
    if (this.pingTimer) {
      clearInterval(this.pingTimer)
      this.pingTimer = null
    }
  }

  private notifyState(state: WSState): void {
    this.onStateChange?.(state, this.latencyMs)
  }
}
