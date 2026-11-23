import {useSyncExternalStore} from 'react'
import {readCart} from './cart'

export function CartIcon({plus=false}:{plus?:boolean}) {
  return <svg aria-hidden="true" viewBox="0 0 32 32" width="26" height="26" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M2 6h4l3.5 14h14l3-10H9"/><circle cx="12" cy="25" r="1.6"/><circle cx="22" cy="25" r="1.6"/>
    {plus&&<g><circle cx="25" cy="7" r="6" fill="white" stroke="none"/><path d="M25 4v6m-3-3h6" stroke="#092d58"/></g>}
  </svg>
}
function subscribe(update:()=>void) {
  window.addEventListener('cartchange',update)
  return ()=>window.removeEventListener('cartchange',update)
}
export function CartLink() {
  const count=useSyncExternalStore(subscribe,()=>readCart().reduce((sum,line)=>sum+line.quantity,0))
  return <a className="cart-link" href="#cart" aria-label={'Giỏ hàng, '+count+' sản phẩm'}>
    <span id="cart-target"><CartIcon/></span>Giỏ hàng <b>{count}</b></a>
}
export function flyToCart(button:HTMLElement,id:number) {
  const target=document.getElementById('cart-target')
  if(!target || matchMedia('(prefers-reduced-motion: reduce)').matches || !button.animate)return
  const start=button.getBoundingClientRect(),end=target.getBoundingClientRect()
  const x=start.left+start.width/2, y=start.top+start.height/2
  const dx=end.left+end.width/2-x,dy=end.top+end.height/2-y
  const item=document.createElement('div')
  item.className='flying-item photo photo-'+id
  item.setAttribute('aria-hidden','true')
  item.style.left=(x-20)+'px';item.style.top=(y-20)+'px'
  document.body.appendChild(item)
  const flight=item.animate([
    {transform:'translate(0,0) scale(.75)'},
    {transform:'translate('+dx*.32+'px,'+(dy*.57-36)+'px) scale(1)',offset:.42},
    {transform:'translate('+dx+'px,'+dy+'px) scale(.25)',opacity:.5}
  ],{duration:620,easing:'ease-in-out'})
  flight.oncancel=()=>item.remove()
  flight.onfinish=()=>{
    item.remove()
    target.animate([{transform:'scale(1)'},{transform:'scale(1.28)'},{transform:'scale(1)'}],{duration:250})
  }
}
