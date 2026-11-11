const methods = [
  { value: 'PAY_LATER', name: 'COD', detail: 'Thanh toán khi nhận hàng. Shop gọi xác nhận đơn trước khi giao hoặc hẹn nhận.' },
  { value: 'VNPAY', name: 'VNPay', detail: 'Thanh toán trực tuyến sau khi gửi đơn.' },
  { value: 'ZALOPAY', name: 'ZaloPay', detail: 'Thanh toán trực tuyến bằng cổng hoặc mã QR.' },
]

export default function PaymentMethodPicker({ id, value, onChange, vnpayAvailable, zalopayAvailable, disabled = false }) {
  return <fieldset className="payment-methods" id={id} disabled={disabled}>
    <legend>Phương thức thanh toán <span>*</span></legend>
    <div className="payment-methods__options">
      {methods.map((method) => {
        const available = method.value === 'VNPAY' ? vnpayAvailable : method.value === 'ZALOPAY' ? zalopayAvailable : true
        return <label key={method.value} className="payment-methods__option">
          <input type="radio" name="paymentMethod" value={method.value} checked={value === method.value}
            onChange={(event) => onChange(event.target.value)} disabled={!available} required />
          <span><strong>{method.name}</strong><small>{available ? method.detail : 'Chưa cấu hình trên máy này.'}</small></span>
        </label>
      })}
    </div>
  </fieldset>
}
