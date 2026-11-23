export async function api<T>(path:string, method='GET', body?:unknown):Promise<T> {
  const response = await fetch('/api/v1'+path, {
    method, headers: { 'Content-Type':'application/json', 'X-CSRF-Protection':'1' },
    body: body === undefined ? undefined : JSON.stringify(body)
  })
  if (!response.ok) {
    const error = await response.json().catch(() => ({}))
    throw new Error(error.detail || 'Yêu cầu thất bại. Vui lòng thử lại.')
  }
  return response.status===204 ? undefined as T : response.json()
}
