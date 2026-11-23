import {useEffect, useState} from 'react'

export type Product = { productId:number; name:string; price:number; availableQuantity:number }
type Line = Product & { quantity:number }
export const currency = new Intl.NumberFormat('vi-VN',{style:'currency',currency:'VND'})
export function readCart():Line[] {
  try {
    const lines = JSON.parse(sessionStorage.getItem('cart') || '[]')
    return Array.isArray(lines) ? lines.filter(l=>l && Number.isInteger(l.productId) && Number.isInteger(l.quantity) && l.quantity>0) : []
  } catch { return [] }
}
export function clearCart() {
  for (const key of ['cart','cartAddress','checkoutLogin']) sessionStorage.removeItem(key)
  window.dispatchEvent(new Event('cartchange'))
}
function saveCart(lines:Line[]) {
  sessionStorage.setItem('cart',JSON.stringify(lines))
  window.dispatchEvent(new Event('cartchange'))
}
export function addProduct(product:Product) {
  const lines = readCart()
  const existing = lines.find(l=>l.productId===product.productId)
  const quantity = (existing?.quantity || 0)+1
  if(quantity>product.availableQuantity) return {added:false,message:'Giỏ đã có đủ số lượng còn bán của '+product.name+'.'}
  const next = {...product,quantity}
  saveCart(existing?lines.map(l=>l===existing?next:l):[...lines,next])
  return {added:true,message:'Đã thêm '+product.name+' vào giỏ.'}
}

export function Cart({user}:{user:{role:string;address:string|null}|null}) {
  const [lines,setLines]=useState(readCart)
  const [address,setAddress]=useState(sessionStorage.getItem('cartAddress') ?? user?.address ?? '')
  const [products,setProducts]=useState<Product[]|null>(null)
  const [message,setMessage]=useState('')
  const [busy,setBusy]=useState(false)
  const [refresh,setRefresh]=useState(0)
  useEffect(()=>{
    const controller=new AbortController()
    fetch('/api/v1/products',{signal:controller.signal}).then(async r=>{
      if(!r.ok) throw Error()
      setProducts(await r.json())
    }).catch(()=>{if(!controller.signal.aborted)setMessage('Chưa tải được tồn kho. Hãy làm mới.')})
    return ()=>controller.abort()
  },[refresh])
  const current=lines.map(line=>{
    const product=products?.find(p=>p.productId===line.productId)
    return {...line,...product,availableQuantity:product?.availableQuantity ?? 0}
  })
  function quantity(id:number,value:number) {
    const next=lines.map(l=>l.productId===id?{...l,quantity:value}:l).filter(l=>l.quantity>0)
    saveCart(next)
    setLines(next)
  }
  function login(){sessionStorage.setItem('checkoutLogin','1');location.hash='login'}
  async function submit(){
    if(!user){login();return}
    setBusy(true)
    try {
      const r=await fetch('/api/v1/orders',{method:'POST',headers:{'Content-Type':'application/json','X-CSRF-Protection':'1'},
        body:JSON.stringify({address,items:lines.map(({productId,quantity})=>({productId,quantity}))})})
      if(r.status===401){login();return}
      const data=await r.json()
      if(!r.ok){setMessage(data.detail || 'Đặt đơn thất bại.');setRefresh(v=>v+1);return}
      clearCart();setLines([]);setAddress('')
      setMessage('Đơn #'+data.orderId+' · Chờ xử lý · '+currency.format(data.total))
    } catch {setMessage('Chưa nhận được xác nhận từ máy chủ.')}
    finally {setBusy(false)}
  }
  return <main className="account checkout"><h1>Giỏ hàng</h1>{message&&<p role="status">{message}</p>}
    {lines.length===0?<p>Giỏ hàng trống. <a href="#catalog">Chọn sản phẩm</a></p>:
    <form onSubmit={e=>{e.preventDefault();void submit()}}>
      <div className="cart-items">{current.map(l=><div className="cart-row" key={l.productId}><div className={'cart-photo photo photo-'+l.productId} aria-hidden="true"/><div><strong>{l.name}</strong><p>{currency.format(l.price)}</p>
        <label>Số lượng<input type="number" min="1" max={products?l.availableQuantity:undefined} value={l.quantity}
          disabled={busy} onChange={e=>{const n=e.target.valueAsNumber;if(Number.isSafeInteger(n)&&n>0)quantity(l.productId,n)}}/></label>
        <small>{products?'Còn có thể đặt: '+l.availableQuantity:'Đang tải tồn kho…'}</small>{' '}
        <button type="button" disabled={busy} onClick={()=>quantity(l.productId,0)}>Xóa</button></div></div>)}</div>
      <strong>Tạm tính {currency.format(current.reduce((sum,l)=>sum+l.price*l.quantity,0))}</strong>
      <label>Địa chỉ giao hàng<input value={address} maxLength={255} required disabled={busy}
        onChange={e=>{setAddress(e.target.value);sessionStorage.setItem('cartAddress',e.target.value)}}/></label>
      <button type="button" disabled={busy} onClick={()=>setRefresh(v=>v+1)}>Làm mới tồn kho</button>
      {user&&user.role!=='CUSTOMER'?<p>Chỉ tài khoản khách hàng được đặt mua.</p>:
      <button disabled={busy||!products||current.some(l=>l.quantity>l.availableQuantity)}>{user?'Xác nhận đặt hàng':'Đăng nhập để đặt hàng'}</button>}
    </form>}</main>
}
