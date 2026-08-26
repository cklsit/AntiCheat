/**
 * 异步随机延迟，模拟网络请求耗时
 * @param ms - 指定毫秒数或区间 [min,max]；默认 200-600ms
 */
export function delay(ms?: number | [number, number]): Promise<void> {
  let actual: number
  if (ms === undefined) {
    actual = Math.floor(200 + Math.random() * 400) // 200-600
  } else if (Array.isArray(ms)) {
    const [min, max] = ms
    actual = Math.floor(min + Math.random() * Math.max(0, max - min))
  } else {
    actual = ms
  }
  return new Promise((resolve) => setTimeout(resolve, Math.max(0, actual)))
}

/**
 * 固定最小延迟包装: 包装 Promise 确保回调至少等待指定 ms
 */
export async function withMinDelay<T>(p: Promise<T>, minMs = 300): Promise<T> {
  const start = Date.now()
  const result = await p
  const elapsed = Date.now() - start
  if (elapsed < minMs) await delay(minMs - elapsed)
  return result
}
