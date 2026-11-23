import {useEffect,useState} from 'react'
import {api} from './api'
import {currency} from './cart'

const labels = {PENDING:'Chờ xử lý',CONFIRMED:'Đã xác nhận',SHIPPING:'Đang giao',COMPLETED:'Hoàn tất',CANCELLED:'Đã hủy'}
const statusColors:Record<string,string> = {PENDING:'status-pending',CONFIRMED:'status-confirmed',SHIPPING:'status-shipping',COMPLETED:'status-completed',CANCELLED:'status-cancelled'}
type Status = keyof typeof labels
type Summary = {orderId:number;status:Status;placedAt:string;address:string;customer:string;handler:string|null;total:number}
type Detail = Summary & {items:{productId:number;name:string;quantity:number;price:number}[];
  serials:{productId:number;serial:string;warrantyUntil:string|null}[]}
const next:Partial<Record<Status,Status>> = {PENDING:'CONFIRMED',CONFIRMED:'SHIPPING',SHIPPING:'COMPLETED'}
const actions:Partial<Record<Status,string>> = {CONFIRMED:'Xác nhận đơn',SHIPPING:'Chuyển sang đang giao',COMPLETED:'Hoàn tất đơn',CANCELLED:'Hủy đơn'}

export function Orders({staff}:{staff:boolean}) {
  const base=staff?'/staff/orders':'/me/orders'
  const [filter,setFilter]=useState(staff?'PENDING':'')
  const [rows,setRows]=useState<Summary[]>([])
  const [id,select]=useState<number|null>(null)
  const [detail,setDetail]=useState<Detail|null>(null)
  const [revision,reload]=useState(0)
  const [busy,setBusy]=useState(false)
  const [loading,setLoading]=useState(true)
  const [message,setMessage]=useState('')
  useEffect(()=>{
    let active=true
    setLoading(true)
    api<Summary[]>(base+'?status='+filter).then(data=>{if(active)setRows(data)})
      .catch(e=>{if(active){setRows([]);setMessage(e.message)}})
      .finally(()=>{if(active)setLoading(false)})
    return ()=>{active=false}
  },[base,filter,revision])
  useEffect(()=>{
    let active=true
    setDetail(null)
    if(id!==null)api<Detail>(base+'/'+id).then(data=>{if(active)setDetail(data)})
      .catch(e=>{if(active)setMessage(e.message)})
    return ()=>{active=false}
  },[base,id,revision])
  async function change(target:Status) {
    if(!detail)return
    if(target==='CANCELLED'&&!window.confirm('Hủy đơn #'+detail.orderId+'?'))return
    setBusy(true)
    try {
      await api(base+'/'+detail.orderId+'/status','PATCH',{status:target})
      setMessage('Đơn #'+detail.orderId+' · '+labels[target])
    } catch(e){setMessage((e as Error).message)}
    finally{setDetail(null);reload(v=>v+1);setBusy(false)}
  }
  const target=detail && (staff?next[detail.status]:['PENDING','CONFIRMED'].includes(detail.status)?'CANCELLED':undefined)
  return <><h1>{staff?'Xử lý đơn':'Đơn của tôi'}</h1>
    <div className="toolbar"><label>Trạng thái <select value={filter} disabled={busy} onChange={e=>{setFilter(e.target.value);select(null);setMessage('')}}>
      <option value="">Tất cả</option>{Object.entries(labels).map(([key,label])=><option key={key} value={key}>{label}</option>)}</select></label>
      <button disabled={busy} onClick={()=>reload(v=>v+1)}>Làm mới</button></div>
    {message&&<p role="status">{message}</p>}
    {detail && <section className="order-detail"><h2>Chi tiết đơn #{detail.orderId}</h2>
      <p><span className={'status-badge '+(statusColors[detail.status]||'')}>{labels[detail.status]}</span> · {detail.customer} · {detail.address}</p>
      {detail.handler&&<p>Người xác nhận: {detail.handler}</p>}
      <ul>{detail.items.map(item=><li key={item.productId}>{item.name} · {item.quantity} × {currency.format(item.price)}
        <ul>{detail.serials.filter(s=>s.productId===item.productId).map(s=><li key={s.serial}>{s.serial}{s.warrantyUntil&&' · Bảo hành đến '+s.warrantyUntil}</li>)}</ul>
      </li>)}</ul>
      <p>Tổng tiền: {currency.format(detail.total)}</p>
      {target&&<button disabled={busy} onClick={()=>void change(target)}>{actions[target]}</button>}
    </section>}
    {loading?<p>Đang tải danh sách…</p>:rows.length===0?<p>Chưa có đơn ở trạng thái này.</p>:
      <ul className="order-list">{rows.map(row=><li key={row.orderId}><button disabled={busy} onClick={()=>{select(row.orderId);setMessage('')}}>Đơn #{row.orderId}</button>
        <span>{new Date(row.placedAt).toLocaleString('vi-VN')} · <span className={'status-badge '+(statusColors[row.status]||'')}>{labels[row.status]}</span> · {currency.format(row.total)}</span></li>)}</ul>}
  </>
}
