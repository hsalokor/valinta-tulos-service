package fi.vm.sade.valintatulosservice.memoize

class TTLOptionalMemoize[-T, +R](f: T => Option[R], lifetimeSeconds: Long) extends (T => Option[R]) {
  private[this] val cache = TTLCache.apply[T, R](lifetimeSeconds, 32)
  def apply(x: T): Option[R] = {
    cache.get(x) match {
      case Some(existingItem) => Some(existingItem)
      case _ =>
        val result = f(x)
        result match {
          case Some(r) =>
            cache.put(x, r)
            result
          case _ => None
        }
    }
  }
}

object TTLOptionalMemoize {
  def memoize[T, R](f: T => Option[R], lifetimeSeconds: Long): (T => Option[R]) = new TTLOptionalMemoize(f, lifetimeSeconds)

  def memoize[T1, T2, R](f: (T1, T2) => Option[R], lifetimeSeconds: Long): ((T1, T2) => Option[R]) =
    Function.untupled(memoize(f.tupled, lifetimeSeconds))

  def memoize[T1, T2, T3, R](f: (T1, T2, T3) => Option[R], lifetimeSeconds: Long): ((T1, T2, T3) => Option[R]) =
    Function.untupled(memoize(f.tupled, lifetimeSeconds))

  def memoize[T1, T2, T3, T4, T5, R](f: (T1, T2, T3, T4, T5) => Option[R], lifetimeSeconds: Long): ((T1, T2, T3, T4, T5) => Option[R]) =
    Function.untupled(memoize(f.tupled, lifetimeSeconds))

  def Y[T, R](f: (T => Option[R]) => T => Option[R], lifetimeSeconds: Long): (T => Option[R]) = {
    lazy val yf: (T => Option[R]) = memoize(f(yf)(_), lifetimeSeconds)
    yf
  }
}
