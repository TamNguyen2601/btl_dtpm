import { useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { Cart, clearCart } from './cart'
import { CartLink } from './cart-motion'
import { Orders } from './orders'
import { api } from './api'

type Account = { id:number; name:string; phone:string; email:string; address:string|null; role:string }
const roles: Record<string,string> = { CUSTOMER:'Khách hàng', STAFF:'Nhân viên', DEALER:'Đại lý', ADMIN:'Quản trị viên' }
const roleColors: Record<string,string> = { CUSTOMER:'role-customer', STAFF:'role-staff', DEALER:'role-dealer', ADMIN:'role-admin' }


function Field({name,label,type='text',value,optional=false}:{name:string;label:string;type?:string;value?:string;optional?:boolean}) {
  return <label>{label}<input type={type} name={name} defaultValue={value} required={!optional}
    minLength={['newPassword','createPassword'].includes(name)?8:undefined}
    maxLength={({name:100,email:100,phone:15,address:255} as Record<string,number>)[name]}/></label>
}

function Form({title,button,submit,children}:{title:string;button:string;submit:(data:Record<string,string>)=>Promise<void>;children:ReactNode}) {
  const [message,setMessage]=useState('')
  const [busy,setBusy]=useState(false)
  async function handle(event:FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form=event.currentTarget
    setBusy(true)
    setMessage('')
    try {
      await submit(Object.fromEntries(new FormData(form)) as Record<string,string>)
      form.querySelectorAll<HTMLInputElement>('input[type=password]').forEach(input=>{input.value=''})
      setMessage('Đã lưu thành công.')
    } catch(error) { setMessage(error instanceof Error?error.message:'Vui lòng thử lại.') }
    finally {setBusy(false)}
  }
  return <form onSubmit={handle}><h2>{title}</h2>{children}<button disabled={busy}>{button}</button>
    {message && <span role="status">{message}</span>}</form>
}

function IdentityFields() {
  return <><Field name="name" label="Họ tên"/><Field name="email" label="Email" type="email"/>
    <Field name="phone" label="Số điện thoại" type="tel"/><Field name="createPassword" label="Mật khẩu (tối thiểu 8 ký tự)" type="password"/></>
}
function RoleSelect({value,internal=false}:{value:string;internal?:boolean}) {
  return <label>Loại tài khoản<select name="role" defaultValue={value}>{Object.entries(roles)
    .filter(([role])=>!internal || role!=='CUSTOMER').map(([role,name])=><option key={role} value={role}>{name}</option>)}</select></label>
}

function AdminAccounts({refreshMe}:{refreshMe:()=>Promise<void>}) {
  const [accounts,setAccounts]=useState<Account[]>([])
  const [error,setError]=useState('')
  async function refresh(){setAccounts(await api<Account[]>('/admin/accounts'));setError('')}
  useEffect(()=>{void refresh().catch(e=>setError(e.message))},[])
  return <><Form title="Tạo tài khoản nội bộ" button="Tạo tài khoản" submit={async data=>{
    await api('/admin/accounts','POST',{...data,password:data.createPassword})
    await refresh()
  }}><IdentityFields/><RoleSelect value="STAFF" internal/></Form>
  <section className="role-list"><h2>Quản lý vai trò</h2>{error && <p role="alert">{error}</p>}
  {accounts.map(a=><Form key={a.id+'-'+a.role} title={a.name+' · '+a.email} button="Lưu vai trò" submit={async data=>{
    await api('/admin/accounts/'+a.id+'/role','PATCH',data)
    await refreshMe()
    await refresh()
  }}><RoleSelect value={a.role}/></Form>)}</section></>
}

export function App({catalog}:{catalog:ReactNode}) {
  const [page,setPage]=useState(location.hash.slice(1)||'catalog')
  const [me,setMe]=useState<Account|null>(null)
  const [error,setError]=useState('')
  const [ready,setReady]=useState(false)
  const go=(page:string)=>{location.hash=page}
  async function refreshMe() {
    const response=await fetch('/api/v1/me')
    if(response.status===401) setMe(null)
    else if(response.ok) setMe(await response.json())
    else throw new Error('Chưa tải được thông tin tài khoản.')
  }
  useEffect(()=>{
    const sync=()=>{void refreshMe().catch(e=>setError(e.message)).finally(()=>setReady(true))}
    const navigate=()=>{setPage(location.hash.slice(1)||'catalog');setError('');sync()}
    sync()
    window.addEventListener('hashchange',navigate)
    window.addEventListener('focus',sync)
    return ()=>{window.removeEventListener('hashchange',navigate);window.removeEventListener('focus',sync)}
  },[])
  return <><header><div className="brand"><a className="home-link" href="#catalog"><img src="/brand.svg" alt=""/><span>Gia Dụng</span></a><nav aria-label="Điều hướng chính">
    <a aria-current={page==='catalog'?'page':undefined} href="#catalog">Cửa hàng</a>
    <CartLink/>
    {me && me.role!=='DEALER' && <a href="#orders">{me.role==='CUSTOMER'?'Đơn của tôi':'Xử lý đơn'}</a>}
    {me?<><a href="#profile">{me.name}</a>{me.role==='ADMIN'&&<a href="#accounts">Tài khoản nội bộ</a>}
      <button onClick={async()=>{try{await api('/sessions/current','DELETE');clearCart();setMe(null);go('catalog')}
        catch(e){setError((e as Error).message)}}}>Đăng xuất</button></>:
      <><a href="#login">Đăng nhập</a><a href="#register">Đăng ký</a></>}
    </nav></div></header>{error&&<p role="alert">{error}</p>}
    {page==='catalog'?catalog:page==='cart'&&ready?<Cart user={me}/>:<main className={'account page-'+(page==='register'?'register':!me?'login':page)}>
      {!ready?<p>Đang tải…</p>:page==='register'?<Form title="Đăng ký khách hàng" button="Đăng ký" submit={async data=>{
        await api('/customers','POST',{...data,password:data.createPassword});go('login')
      }}><IdentityFields/></Form>:page==='login'||!me?<Form title="Đăng nhập" button="Đăng nhập" submit={async data=>{
        setMe(await api<Account>('/sessions','POST',data))
        const destination=sessionStorage.getItem('checkoutLogin')?'cart':'catalog'
        sessionStorage.removeItem('checkoutLogin');go(destination)
      }}><Field name="email" label="Email" type="email"/><Field name="password" label="Mật khẩu" type="password"/></Form>:
      page==='accounts'&&me.role==='ADMIN'?<AdminAccounts refreshMe={refreshMe}/>:
      page==='orders'&&me.role!=='DEALER'?<Orders key={me.id+'-'+me.role} staff={me.role!=='CUSTOMER'}/>:
      <><Form title="Hồ sơ cá nhân" button="Lưu hồ sơ" submit={async data=>setMe(await api<Account>('/me','PATCH',data))}>
        <span>{me.email} · <span className={'role-badge '+(roleColors[me.role]||'')}>{roles[me.role]}</span></span><Field name="name" label="Họ tên" value={me.name}/>
        <Field name="phone" label="Số điện thoại" value={me.phone}/><Field name="address" label="Địa chỉ" value={me.address||''} optional/>
      </Form><Form title="Đổi mật khẩu" button="Lưu mật khẩu" submit={async data=>{await api('/me/password','PUT',data)}}>
        <Field name="oldPassword" label="Mật khẩu cũ" type="password"/><Field name="newPassword" label="Mật khẩu mới" type="password"/>
      </Form></>}
    </main>}<footer><a href="#catalog">Gia Dụng</a><a href="#cart">Giỏ hàng ↗</a></footer></>
}
