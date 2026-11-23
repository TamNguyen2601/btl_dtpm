import { useEffect, useState } from 'react'
import { createRoot } from 'react-dom/client'
import './style.css'
import { App } from './accounts'
import {CartIcon,flyToCart} from './cart-motion'

import {addProduct, currency, type Product} from './cart'
type CatalogState =
  | { status: 'loading' | 'failed' }
  | { status: 'ready'; products: Product[] }


function Catalog() {
  const [inStock,setInStock]=useState(false)
  const [search,setSearch]=useState('')
  const [message,setMessage]=useState('')
  const [state, setState] = useState<CatalogState>({ status: 'loading' })
  const [request, setRequest] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    async function load() {
      try {
        const response = await fetch('/api/v1/products', { signal: controller.signal })
        if (!response.ok) throw new Error('catalog unavailable')
        const products: Product[] = await response.json()
        if (!controller.signal.aborted) setState({ status: 'ready', products })
      } catch {
        if (!controller.signal.aborted) setState({ status: 'failed' })
      }
    }
    void load()
    return () => controller.abort()
  }, [request])

  function reload() {
    setState({ status: 'loading' })
    setRequest(value => value + 1)
  }

  const products=state.status==='ready'?state.products.filter(p=>(!inStock||p.availableQuantity>0)&&p.name.toLocaleLowerCase('vi').includes(search.trim().toLocaleLowerCase('vi'))):[]
  return <>
    <main className="storefront">
      <div className="showcase"><div className="showcase-copy"><h1>Đồ dùng<br/><em>nhà bếp.</em></h1>
        <button onClick={()=>document.getElementById('collection')?.scrollIntoView({block:'start'})}>Khám phá sản phẩm <span aria-hidden="true">↓</span></button>
      </div><div className="showcase-photo" role="img" aria-label="Ấm nước và bát sứ trên bàn bếp"/></div>
      <div id="collection" className="collection-title"><h2>Danh mục sản phẩm</h2>
        <input type="search" aria-label="Tìm sản phẩm" placeholder="Tìm trong cửa hàng…" value={search} onChange={e=>setSearch(e.target.value)}/>
      </div>
      <div className="catalog-tools"><div><button aria-pressed={!inStock} onClick={()=>setInStock(false)}>Tất cả</button><button aria-pressed={inStock} onClick={()=>setInStock(true)}>Còn hàng</button></div>
      <button disabled={state.status==='loading'} onClick={reload}>↻ Làm mới</button></div>
      <section className="catalog" aria-label="Sản phẩm đang bán" aria-live="polite">
        {message&&<p className="cart-notice" role="status">{message} <a href="#cart">Xem giỏ hàng</a><button aria-label="Đóng thông báo" onClick={()=>setMessage('')}>×</button></p>}
        {state.status === 'loading' && <p>Đang tải sản phẩm…</p>}
        {state.status === 'failed' && <p role="alert">Chưa tải được danh sách. Hãy kiểm tra kết nối rồi chọn Làm mới.</p>}
        {state.status === 'ready' && (products.length === 0
          ? <p>{search||inStock?'Không tìm thấy sản phẩm phù hợp.':'Chưa có sản phẩm mở bán.'}</p>
          : <div className="product-grid">{products.map(item=><article className="product" key={item.productId}>
              <div className="product-picture"><div aria-hidden="true" className={'photo photo-'+item.productId}/>
              <small className={'stock-sticker '+(item.availableQuantity?'stock':'sold-out')}>{item.availableQuantity?'Còn '+item.availableQuantity+' chiếc':'Hết hàng'}</small></div>
              <div className="product-info">
              <h3>{item.name}</h3><strong>{currency.format(item.price)}</strong>
              <button className="add-cart" aria-label={'Thêm '+item.name+' vào giỏ'} title="Thêm vào giỏ" disabled={item.availableQuantity<1} onClick={event=>{
                const result=addProduct(item)
                if(result.added)flyToCart(event.currentTarget,item.productId)
                setMessage(result.message)
              }}><CartIcon plus/></button></div>
            </article>)}</div>)}
      </section>
    </main>
  </>
}

createRoot(document.getElementById('root')!).render(<App catalog={<Catalog />}/>)
