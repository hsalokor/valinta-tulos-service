package fi.vm.sade.valintatulosservice.memoize

class TTLOptionalMemoize[-T, +R](f: T => Either[Throwable, R], lifetimeSeconds: Long) extends (T => Either[Throwable, R]) {
  private[this] val cache = TTLCache.apply[T, R](lifetimeSeconds, 32)
  def apply(x: T): Either[Throwable, R] = {
    cache.get(x) match {
      case Some(existingItem) => Right(existingItem)
      case None =>
        val result = f(x)
        result.right.foreach(cache.put(x, _))
        result
    }
  }
}

object TTLOptionalMemoize {
  def memoize[T, R](f: T => Either[Throwable, R], lifetimeSeconds: Long): (T => Either[Throwable, R]) = new TTLOptionalMemoize(f, lifetimeSeconds)

  def memoize[T1, T2, R](f: (T1, T2) => Either[Throwable, R], lifetimeSeconds: Long): ((T1, T2) => Either[Throwable, R]) =
    Function.untupled(memoize(f.tupled, lifetimeSeconds))

  def memoize[T1, T2, T3, R](f: (T1, T2, T3) => Either[Throwable, R], lifetimeSeconds: Long): ((T1, T2, T3) => Either[Throwable, R]) =
    Function.untupled(memoize(f.tupled, lifetimeSeconds))

  def memoize[T1, T2, T3, T4, T5, R](f: (T1, T2, T3, T4, T5) => Either[Throwable, R], lifetimeSeconds: Long): ((T1, T2, T3, T4, T5) => Either[Throwable, R]) =
    Function.untupled(memoize(f.tupled, lifetimeSeconds))
}
