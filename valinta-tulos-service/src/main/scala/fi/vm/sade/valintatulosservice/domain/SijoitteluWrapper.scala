package fi.vm.sade.valintatulosservice.domain

import java.util.Date

import fi.vm.sade.sijoittelu.domain.{Valintatapajono, Valintatulos, Hakukohde, SijoitteluAjo}
import fi.vm.sade.sijoittelu.domain.{Tasasijasaanto => SijoitteluTasasijasaanto}

case class SijoitteluWrapper(sijoitteluajo:SijoitteluAjo, hakukohteet:List[Hakukohde], valintatulokset:List[Valintatulos])

case class SijoitteluajoWrapper(sijoitteluajoId:Long, hakuOid:String, startMils:Long, endMils:Long) {
  val sijoitteluajo:SijoitteluAjo = {
    val sijoitteluajo = new SijoitteluAjo
    sijoitteluajo.setSijoitteluajoId(sijoitteluajoId)
    sijoitteluajo.setHakuOid(hakuOid)
    sijoitteluajo.setStartMils(startMils)
    sijoitteluajo.setEndMils(endMils)
    sijoitteluajo
  }
}

object SijoitteluajoWrapper {
  def apply(sijoitteluAjo: SijoitteluAjo): SijoitteluajoWrapper = {
    SijoitteluajoWrapper(sijoitteluAjo.getSijoitteluajoId, sijoitteluAjo.getHakuOid, sijoitteluAjo.getStartMils, sijoitteluAjo.getEndMils)
  }
}

case class SijoitteluajonHakukohdeWrapper(sijoitteluajoId:Long, oid:String, tarjoajaOid:String, kaikkiJonotSijoiteltu:Boolean) {
  val hakukohde:Hakukohde = {
    val hakukohde = new Hakukohde
    hakukohde.setSijoitteluajoId(sijoitteluajoId)
    hakukohde.setOid(oid)
    hakukohde.setTarjoajaOid(tarjoajaOid)
    hakukohde.setKaikkiJonotSijoiteltu(kaikkiJonotSijoiteltu)
    hakukohde
  }
}

object SijoitteluajonHakukohdeWrapper {
  def apply(hakukohde: Hakukohde): SijoitteluajonHakukohdeWrapper = {
    SijoitteluajonHakukohdeWrapper(hakukohde.getSijoitteluajoId, hakukohde.getOid, hakukohde.getTarjoajaOid, hakukohde.isKaikkiJonotSijoiteltu)
  }
}

sealed trait Tasasijasaanto {
  def tasasijasaanto:SijoitteluTasasijasaanto
}

case object Arvonta extends Tasasijasaanto {
  val tasasijasaanto = SijoitteluTasasijasaanto.ARVONTA
}

case object Ylitaytto extends Tasasijasaanto {
  val tasasijasaanto = SijoitteluTasasijasaanto.YLITAYTTO
}

case object Alitaytto extends Tasasijasaanto {
  val tasasijasaanto = SijoitteluTasasijasaanto.ALITAYTTO
}

object Tasasijasaanto {
  private val valueMapping = Map(
    "Arvonta" -> Arvonta,
    "Ylitaytto" -> Ylitaytto,
    "Alitaytto" -> Alitaytto)
  val values: List[String] = valueMapping.keysIterator.toList
  def apply(value: String): Tasasijasaanto = valueMapping.getOrElse(value, {
    throw new IllegalArgumentException(s"Unknown tasasijasaanto '$value', expected one of $values")
  })
  def getTasasijasaanto(tasasijasaanto: SijoitteluTasasijasaanto) = tasasijasaanto match {
    case SijoitteluTasasijasaanto.ARVONTA => Arvonta
    case SijoitteluTasasijasaanto.ALITAYTTO => Alitaytto
    case SijoitteluTasasijasaanto.YLITAYTTO => Ylitaytto
    case null => Arvonta
    case x => throw new IllegalArgumentException(s"Tasasijasaanto ${x} ei ole sallittu")
  }
}

case class SijoitteluajonValintatapajonoWrapper(
  oid:String,
  nimi:String,
  prioriteetti:Int,
  tasasijasaanto:Tasasijasaanto,
  aloituspaikat:Int,
  alkuperaisetAloituspaikat:Option[Int],
  eiVarasijatayttoa:Boolean,
  kaikkiEhdonTayttavatHyvaksytaan:Boolean,
  poissaOlevaTaytto:Boolean,
  varasijat:Int = 0,
  varasijaTayttoPaivat:Int = 0,
  varasijojaKaytetaanAlkaen:Option[Date],
  varasijojaTaytetaanAsti:Option[Date],
  tayttojono:Option[String],
  hyvaksytty:Option[Int],
  varalla:Option[Int],
  alinHyvaksyttyPistemaara:Option[BigDecimal],
  valintaesitysHyvaksytty:Option[Boolean]) {

  val valintatapajono:Valintatapajono = {
    val valintatapajono = new Valintatapajono
    valintatapajono.setOid(oid)
    valintatapajono.setNimi(nimi)
    valintatapajono.setPrioriteetti(prioriteetti)
    valintatapajono.setTasasijasaanto(tasasijasaanto.tasasijasaanto)
    valintatapajono.setAloituspaikat(aloituspaikat)
    alkuperaisetAloituspaikat.foreach(valintatapajono.setAlkuperaisetAloituspaikat(_))
    valintatapajono.setEiVarasijatayttoa(eiVarasijatayttoa)
    valintatapajono.setKaikkiEhdonTayttavatHyvaksytaan(kaikkiEhdonTayttavatHyvaksytaan)
    valintatapajono.setPoissaOlevaTaytto(poissaOlevaTaytto)
    valintatapajono.setVarasijat(varasijat)
    valintatapajono.setVarasijaTayttoPaivat(varasijaTayttoPaivat)
    varasijojaKaytetaanAlkaen.foreach(valintatapajono.setVarasijojaKaytetaanAlkaen(_))
    varasijojaTaytetaanAsti.foreach(valintatapajono.setVarasijojaTaytetaanAsti(_))
    tayttojono.foreach(valintatapajono.setTayttojono(_))
    hyvaksytty.foreach(valintatapajono.setHyvaksytty(_))
    varalla.foreach(valintatapajono.setVaralla(_))
    alinHyvaksyttyPistemaara.foreach(pm => valintatapajono.setAlinHyvaksyttyPistemaara(pm.bigDecimal))
    valintaesitysHyvaksytty.foreach(valintatapajono.setValintaesitysHyvaksytty(_))
    valintatapajono
  }
}

object SijoitteluajonValintatapajonoWrapper {
  type javaInt = java.lang.Integer
  type javaBigDecimal = java.math.BigDecimal
  type javaBoolean = java.lang.Boolean
  type javaString = java.lang.String

  def int(x:javaInt) = x.toInt
  def boolean(x:javaBoolean) = x.booleanValue
  def bigDecimal(x:javaBigDecimal) = BigDecimal(x)
  def string(x:javaString) = x

  private def convert[javaType,scalaType](javaObject:javaType, f:javaType => scalaType):Option[scalaType] = javaObject match {
    case null => None //Avoid NullPointerException raised by type conversion when creating scala option with java object
    case x => Some(f(x))
  }

  def apply(valintatapajono:Valintatapajono): SijoitteluajonValintatapajonoWrapper = {
    SijoitteluajonValintatapajonoWrapper(
      valintatapajono.getOid(),
      valintatapajono.getNimi(),
      valintatapajono.getPrioriteetti(),
      Tasasijasaanto.getTasasijasaanto(valintatapajono.getTasasijasaanto),
      valintatapajono.getAloituspaikat(),
      convert[javaInt,Int](valintatapajono.getAlkuperaisetAloituspaikat(), int),
      valintatapajono.getEiVarasijatayttoa(),
      valintatapajono.getKaikkiEhdonTayttavatHyvaksytaan(),
      valintatapajono.getPoissaOlevaTaytto(),
      valintatapajono.getVarasijat(),
      valintatapajono.getVarasijaTayttoPaivat(),
      Option(valintatapajono.getVarasijojaKaytetaanAlkaen()),
      Option(valintatapajono.getVarasijojaTaytetaanAsti()),
      convert[javaString,String](valintatapajono.getTayttojono, string),
      convert[javaInt,Int](valintatapajono.getHyvaksytty(), int),
      convert[javaInt,Int](valintatapajono.getVaralla(), int),
      convert[javaBigDecimal,BigDecimal](valintatapajono.getAlinHyvaksyttyPistemaara(), bigDecimal),
      convert[javaBoolean,Boolean](valintatapajono.getValintaesitysHyvaksytty(), boolean)
    )
  }
}
