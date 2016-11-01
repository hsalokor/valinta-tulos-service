package fi.vm.sade.valintatulosservice.domain

import java.lang.{Boolean => javaBoolean, Integer => javaInt, String => javaString}
import java.math.{BigDecimal => javaBigDecimal}
import java.util.Date

import fi.vm.sade.sijoittelu.domain.{Hakemus => SijoitteluHakemus, Tasasijasaanto => SijoitteluTasasijasaanto, _}

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

object SijoitteluajonValintatapajonoWrapper extends OptionConverter {
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

sealed trait Valinnantila {
  def valinnantila:HakemuksenTila
}

case object Hylatty extends Valinnantila {
  val valinnantila = HakemuksenTila.HYLATTY
}

case object Varalla extends Valinnantila {
  val valinnantila = HakemuksenTila.VARALLA
}

case object Peruuntunut extends Valinnantila {
  val valinnantila = HakemuksenTila.PERUUNTUNUT
}

case object Perunut extends Valinnantila {
  val valinnantila = HakemuksenTila.PERUNUT
}

case object Peruutettu extends Valinnantila {
  val valinnantila = HakemuksenTila.PERUUTETTU
}

case object Hyvaksytty extends Valinnantila {
  val valinnantila = HakemuksenTila.HYVAKSYTTY
}

case object VarasijaltaHyvaksytty extends Valinnantila {
  val valinnantila = HakemuksenTila.VARASIJALTA_HYVAKSYTTY
}

object Valinnantila {
  private val valueMapping = Map(
    "Hylatty" -> Hylatty,
    "Varalla" -> Varalla,
    "Peruuntunut" -> Peruuntunut,
    "VarasijaltaHyvaksytty" -> VarasijaltaHyvaksytty,
    "Hyvaksytty" -> Hyvaksytty,
    "Perunut" -> Perunut,
    "Peruutettu" -> Peruutettu)
  val values: List[String] = valueMapping.keysIterator.toList
  def apply(value: String): Valinnantila = valueMapping.getOrElse(value, {
    throw new IllegalArgumentException(s"Unknown valinnantila '$value', expected one of $values")
  })
  def getValinnantila(valinnantila:HakemuksenTila) = valinnantila match {
    case HakemuksenTila.HYLATTY => Hylatty
    case HakemuksenTila.HYVAKSYTTY => Hyvaksytty
    case HakemuksenTila.PERUNUT => Perunut
    case HakemuksenTila.PERUUNTUNUT => Peruuntunut
    case HakemuksenTila.PERUUTETTU => Peruutettu
    case HakemuksenTila.VARALLA => Varalla
    case HakemuksenTila.VARASIJALTA_HYVAKSYTTY => VarasijaltaHyvaksytty
    case null => throw new IllegalArgumentException(s"Valinnantila null ei ole sallittu")
  }
}

sealed trait ValinnantilanTarkenne {
  def valinnantilanTarkenne:Map[String,String]
}

case object PeruuntunutHyvaksyttyYlemmalleHakutoiveelle extends ValinnantilanTarkenne {
  val valinnantilanTarkenne = Map("FI" -> "Peruuntunut, hyväksytty ylemmälle hakutoiveelle",
    "SV" -> "Annullerad, godkänt till ansökningsmål med högre prioritet",
    "EN" -> "Cancelled, accepted for a study place with higher priority")
}
case object PeruuntunutAloituspaikatTaynna extends ValinnantilanTarkenne {
  val valinnantilanTarkenne = Map("FI" -> "Peruuntunut, aloituspaikat täynnä",
    "SV" -> "Annullerad, nybörjarplatser fyllda",
    "EN" -> "Cancelled, study places are filled")
}
case object PeruuntunutHyvaksyttyToisessaJonossa extends ValinnantilanTarkenne {
  val valinnantilanTarkenne = Map("FI" -> "Peruuntunut, hyväksytty toisessa valintatapajonossa",
    "SV" -> "Annullerad, godkänd i en annan urvalsmetodskö",
    "EN" -> "Cancelled, accepted in another selection method queue")
}
case object HyvaksyttyVarasijalta extends ValinnantilanTarkenne {
  val valinnantilanTarkenne = Map("FI" -> "Varasijalta hyväksytty",
    "SV" -> "Godkänd från reservplats",
    "EN" -> "Accepted from a reserve place")
}
case object PeruuntunutEiVastaanottanutMaaraaikana extends ValinnantilanTarkenne {
  val valinnantilanTarkenne = Map("FI" -> "Peruuntunut, ei vastaanottanut määräaikana",
    "SV" -> "Annullerad, har inte tagit emot platsen inom utsatt tid",
    "EN" -> "Cancelled, has not confirmed the study place within the deadline")
}
case object PeruuntunutVastaanottanutToisenPaikan extends ValinnantilanTarkenne {
  val valinnantilanTarkenne = Map("FI" -> "Peruuntunut, ottanut vastaan toisen opiskelupaikan",
    "SV" -> "Annullerad, ottanut vastaan toisen opiskelupaikan",
    "EN" -> "Cancelled, ottanut vastaan toisen opiskelupaikan")
}
case object PeruuntunutEiMahduVarasijojenMaaraan extends ValinnantilanTarkenne {
  val valinnantilanTarkenne = Map("FI" -> "Peruuntunut, varasija ei mahdu käsiteltävien varasijojen määrään",
    "SV" -> "Annullerad, reservplatsen ryms inte med i antalet reservplatser",
    "EN" -> "Cancelled, the reserve place does not fit into the amount of processed reserve places")
}
case object PeruuntunutHakukierrosPaattynyt extends ValinnantilanTarkenne {
  val valinnantilanTarkenne = Map("FI" -> "Peruuntunut, varasijatäyttö päättynyt",
    "SV" -> "Annullerad, besättning av reservplatser har upphört",
    "EN" -> "Cancelled, reserve place selection has ended")
}
case object PeruuntunutEiVarasijatayttoa extends ValinnantilanTarkenne {
  val valinnantilanTarkenne = Map("FI" -> "Peruuntunut, varasijatäyttö päättynyt",
    "SV" -> "Annullerad, besättning av reservplatser har upphört",
    "EN" -> "Cancelled, reserve place selection has ended")
}
case object HyvaksyttyTayttojonoSaannolla extends ValinnantilanTarkenne {
  val valinnantilanTarkenne = Map("FI" -> "Hyväksytty täyttöjonosäännöllä valintatapajonosta: ",
    "SV" -> "Godkänd med köpåfyllningsregel från urvalsmetodskö: ",
    "EN" -> "Accepted from selection method queue: ")
}
case object HylattyHakijaryhmaanKuulumattomana extends ValinnantilanTarkenne {
  val valinnantilanTarkenne = Map("FI" -> "Hylätty, ei kuulu hakijaryhmään: ",
    "SV" -> "Annullerad, hör inte till gruppen för sökande: ",
    "EN" -> "Cancelled, is not included in the applicant group: ")
}
case object PeruuntunutVastaanottanutToisenPaikanYhdenSaannonPaikanPiirissa extends ValinnantilanTarkenne {
  val valinnantilanTarkenne = Map("FI" -> "Peruuntunut, vastaanottanut toisen korkeakoulupaikan",
    "SV" -> "Annullerad, tagit emot en annan högskoleplats",
    "EN" -> "Cancelled, accepted another higher education study place")
}

object ValinnantilanTarkenne {
  private val valueMapping = Map(
    "peruuntunutHyvaksyttyYlemmalleHakutoiveelle" -> PeruuntunutHyvaksyttyYlemmalleHakutoiveelle,
    "peruuntunutAloituspaikatTaynna" -> PeruuntunutAloituspaikatTaynna,
    "peruuntunutHyvaksyttyToisessaJonossa" -> PeruuntunutHyvaksyttyToisessaJonossa,
    "hyvaksyttyVarasijalta" -> HyvaksyttyVarasijalta,
    "peruuntunutEiVastaanottanutMaaraaikana" -> PeruuntunutEiVastaanottanutMaaraaikana,
    "peruuntunutVastaanottanutToisenPaikan" -> PeruuntunutVastaanottanutToisenPaikan,
    "peruuntunutEiMahduVarasijojenMaaraan" -> PeruuntunutEiMahduVarasijojenMaaraan,
    "peruuntunutHakukierrosPaattynyt" -> PeruuntunutHakukierrosPaattynyt,
    "peruuntunutEiVarasijatayttoa" -> PeruuntunutEiVarasijatayttoa,
    "hyvaksyttyTayttojonoSaannolla" -> HyvaksyttyTayttojonoSaannolla,
    "hylattyHakijaryhmaanKuulumattomana" -> HylattyHakijaryhmaanKuulumattomana,
    "peruuntunutVastaanottanutToisenPaikanYhdenSaannonPaikanPiirissa" -> PeruuntunutVastaanottanutToisenPaikanYhdenSaannonPaikanPiirissa
  )
  val values: List[String] = valueMapping.keysIterator.toList
  def apply(value: String): ValinnantilanTarkenne = valueMapping.getOrElse(value, {
    throw new IllegalArgumentException(s"Unknown valinnantilan tarkenne '$value', expected one of $values")
  })
  def getValinnantilanTarkenne(tilanKuvaus:Map[String,String]) = tilanKuvaus.get("FI") match {
    case Some("Peruuntunut, hyväksytty ylemmälle hakutoiveelle") => Some(PeruuntunutHyvaksyttyYlemmalleHakutoiveelle)
    case Some("Peruuntunut, aloituspaikat täynnä") => Some(PeruuntunutAloituspaikatTaynna)
    case Some("Peruuntunut, hyväksytty toisessa valintatapajonossa") => Some(PeruuntunutHyvaksyttyToisessaJonossa)
    case Some("Varasijalta hyväksytty") => Some(HyvaksyttyVarasijalta)
    case Some("Peruuntunut, ei vastaanottanut määräaikana") => Some(PeruuntunutEiVastaanottanutMaaraaikana)
    case Some("Peruuntunut, ottanut vastaan toisen opiskelupaikan") => Some(PeruuntunutVastaanottanutToisenPaikan)
    case Some("Peruuntunut, varasija ei mahdu käsiteltävien varasijojen määrään") => Some(PeruuntunutEiMahduVarasijojenMaaraan)
    case Some("Peruuntunut, varasijatäyttö päättynyt") => Some(PeruuntunutHakukierrosPaattynyt)
    case Some("Peruuntunut, ei varasijatäyttöä") => Some(PeruuntunutEiVarasijatayttoa)
    case Some(x) if x.startsWith("Hyväksytty täyttöjonosäännöllä valintatapajonosta:") => Some(HyvaksyttyTayttojonoSaannolla)
    case Some(x) if x.startsWith("Hylätty, ei kuulu hakijaryhmään:") => Some(HylattyHakijaryhmaanKuulumattomana)
    case Some("Peruuntunut, vastaanottanut toisen korkeakoulupaikan") => Some(PeruuntunutVastaanottanutToisenPaikanYhdenSaannonPaikanPiirissa)
    case _ => None
  }
}

case class SijoitteluajonHakemusWrapper(
  hakemusOid:String,
  hakijaOid:String,
  etunimi:String,
  sukunimi:String,
  prioriteetti:Int,
  jonosija:Int,
  varasijanNumero:Option[Int],
  onkoMuuttunutViimeSijoittelussa:Option[Boolean],
  pisteet:Option[BigDecimal],
  tasasijaJonosija:Option[Int],
  hyvaksyttyHarkinnanvaraisesti:Option[Boolean],
  hyvaksyttyHakijaryhmasta:Option[Boolean],
  siirtynytToisestaValintatapajonosta:Option[Boolean],
  tila:Valinnantila,
  valinnanTilanTarkenne:Option[ValinnantilanTarkenne]) {

  import scala.collection.JavaConverters._

  val hakemus:SijoitteluHakemus = {
    val hakemus = new SijoitteluHakemus
    hakemus.setHakemusOid(hakemusOid)
    hakemus.setHakijaOid(hakijaOid)
    hakemus.setEtunimi(etunimi)
    hakemus.setSukunimi(sukunimi)
    hakemus.setPrioriteetti(prioriteetti)
    hakemus.setJonosija(jonosija)
    varasijanNumero.foreach(hakemus.setVarasijanNumero(_))
    onkoMuuttunutViimeSijoittelussa.foreach(hakemus.setOnkoMuuttunutViimeSijoittelussa(_))
    pisteet.foreach(p => hakemus.setPisteet(p.bigDecimal))
    tasasijaJonosija.foreach(hakemus.setTasasijaJonosija(_))
    hyvaksyttyHarkinnanvaraisesti.foreach(hakemus.setHyvaksyttyHarkinnanvaraisesti(_))
    hyvaksyttyHakijaryhmasta.foreach(hakemus.setHyvaksyttyHakijaryhmasta(_))
    siirtynytToisestaValintatapajonosta.foreach(hakemus.setSiirtynytToisestaValintatapajonosta(_))
    hakemus.setTila(tila.valinnantila)
    valinnanTilanTarkenne.foreach(tarkenne => hakemus.setTilanKuvaukset(tarkenne.valinnantilanTarkenne.asJava))
    hakemus
  }
}

object SijoitteluajonHakemusWrapper extends OptionConverter {
  import scala.collection.JavaConverters._
  def apply(hakemus:SijoitteluHakemus):SijoitteluajonHakemusWrapper = {
    SijoitteluajonHakemusWrapper(
      hakemus.getHakemusOid,
      hakemus.getHakijaOid,
      hakemus.getEtunimi,
      hakemus.getSukunimi,
      hakemus.getPrioriteetti,
      hakemus.getJonosija,
      convert[javaInt,Int](hakemus.getVarasijanNumero,int),
      convert[javaBoolean,Boolean](hakemus.isOnkoMuuttunutViimeSijoittelussa,boolean),
      convert[javaBigDecimal,BigDecimal](hakemus.getPisteet, bigDecimal),
      convert[javaInt,Int](hakemus.getTasasijaJonosija,int),
      convert[javaBoolean,Boolean](hakemus.isHyvaksyttyHarkinnanvaraisesti,boolean),
      convert[javaBoolean,Boolean](hakemus.isHyvaksyttyHakijaryhmasta,boolean),
      convert[javaBoolean,Boolean](hakemus.getSiirtynytToisestaValintatapajonosta,boolean),
      Valinnantila.getValinnantila(hakemus.getTila),
      ValinnantilanTarkenne.getValinnantilanTarkenne(hakemus.getTilanKuvaukset.asScala.toMap)
    )
  }
}

sealed trait SijoitteluajonIlmoittautumistila {
  def ilmoittautumistila:IlmoittautumisTila
}

case object EiTehty extends SijoitteluajonIlmoittautumistila {
  val ilmoittautumistila = IlmoittautumisTila.EI_TEHTY
}

case object LasnaKokoLukuvuosi extends SijoitteluajonIlmoittautumistila {
  val ilmoittautumistila = IlmoittautumisTila.LASNA_KOKO_LUKUVUOSI
}

case object PoissaKokoLukuvuosi extends SijoitteluajonIlmoittautumistila {
  val ilmoittautumistila = IlmoittautumisTila.POISSA_KOKO_LUKUVUOSI
}

case object EiIlmoittautunut extends SijoitteluajonIlmoittautumistila {
  val ilmoittautumistila = IlmoittautumisTila.EI_ILMOITTAUTUNUT
}

case object LasnaSyksy extends SijoitteluajonIlmoittautumistila {
  val ilmoittautumistila = IlmoittautumisTila.LASNA_SYKSY
}

case object PoissaSyksy extends SijoitteluajonIlmoittautumistila {
  val ilmoittautumistila = IlmoittautumisTila.POISSA_SYKSY
}

case object Lasna extends SijoitteluajonIlmoittautumistila {
  val ilmoittautumistila = IlmoittautumisTila.LASNA
}

case object Poissa extends SijoitteluajonIlmoittautumistila {
  val ilmoittautumistila = IlmoittautumisTila.POISSA
}

object SijoitteluajonIlmoittautumistila {
  private val valueMapping = Map(
    "EiTehty" -> EiTehty,
    "LasnaKokoLukuvuosi" -> LasnaKokoLukuvuosi,
    "PoissaKokoLukuvuosi" -> PoissaKokoLukuvuosi,
    "EiIlmoittautunut" -> EiIlmoittautunut,
    "LasnaSyksy" -> LasnaSyksy,
    "PoissaSyksy" -> PoissaSyksy,
    "Lasna" -> Lasna,
    "Poissa" -> Poissa)
  val values: List[String] = valueMapping.keysIterator.toList
  def apply(value: String): SijoitteluajonIlmoittautumistila = valueMapping.getOrElse(value, {
    throw new IllegalArgumentException(s"Unknown ilmoittautumistila '$value', expected one of $values")
  })
  def getIlmoittautumistila(ilmoittautumistila: IlmoittautumisTila) = ilmoittautumistila match {
    case IlmoittautumisTila.EI_TEHTY => EiTehty
    case IlmoittautumisTila.LASNA_KOKO_LUKUVUOSI => LasnaKokoLukuvuosi
    case IlmoittautumisTila.POISSA_KOKO_LUKUVUOSI => PoissaKokoLukuvuosi
    case IlmoittautumisTila.EI_ILMOITTAUTUNUT => EiIlmoittautunut
    case IlmoittautumisTila.LASNA_SYKSY => LasnaSyksy
    case IlmoittautumisTila.POISSA_SYKSY => PoissaSyksy
    case IlmoittautumisTila.LASNA => Lasna
    case IlmoittautumisTila.POISSA => Poissa
    case null => EiTehty
  }
}

case class SijoitteluajonValinnantulosWrapper(
  valintatapajonoOid:String,
  hakemusOid:String,
  hakukohdeOid:String,
  ehdollisestiHyvaksyttavissa:Boolean = false,
  julkaistavissa:Boolean = false,
  hyvaksyttyVarasijalta:Boolean = false,
  hyvaksyPeruuntunut:Boolean = false,
  ilmoittautumistila:Option[SijoitteluajonIlmoittautumistila]
) {
  val valintatulos:Valintatulos = {
    val valintatulos = new Valintatulos()
    valintatulos.setValintatapajonoOid(valintatapajonoOid, "")
    valintatulos.setHakemusOid(hakemusOid, "")
    valintatulos.setHakukohdeOid(hakukohdeOid, "")
    valintatulos.setEhdollisestiHyvaksyttavissa(ehdollisestiHyvaksyttavissa, "", "");
    valintatulos.setJulkaistavissa(julkaistavissa, "")
    valintatulos.setHyvaksyttyVarasijalta(hyvaksyttyVarasijalta, "")
    valintatulos.setHyvaksyPeruuntunut(hyvaksyPeruuntunut, "")
    ilmoittautumistila.foreach(ilmoittautumistila => valintatulos.setIlmoittautumisTila(ilmoittautumistila.ilmoittautumistila, ""))
    valintatulos
  }
}

object SijoitteluajonValinnantulosWrapper extends OptionConverter {
  def apply(valintatulos:Valintatulos):SijoitteluajonValinnantulosWrapper = {
    SijoitteluajonValinnantulosWrapper(
      valintatulos.getValintatapajonoOid,
      valintatulos.getHakemusOid,
      valintatulos.getHakukohdeOid,
      valintatulos.getEhdollisestiHyvaksyttavissa,
      valintatulos.getJulkaistavissa,
      valintatulos.getHyvaksyttyVarasijalta,
      valintatulos.getHyvaksyPeruuntunut,
      convert[IlmoittautumisTila,SijoitteluajonIlmoittautumistila](valintatulos.getIlmoittautumisTila,
        SijoitteluajonIlmoittautumistila.getIlmoittautumistila)
    )
  }
}

case class SijoitteluajonPistetietoWrapper(
  tunniste:String,
  arvo:Option[String],
  laskennallinenArvo:Option[String],
  osallistuminen:Option[String]
) {
  val pistetieto:Pistetieto = {
    val pistetieto = new Pistetieto()
    pistetieto.setTunniste(tunniste)
    arvo.foreach(pistetieto.setArvo(_))
    laskennallinenArvo.foreach(pistetieto.setLaskennallinenArvo(_))
    osallistuminen.foreach(pistetieto.setOsallistuminen(_))
    pistetieto
  }
}

object SijoitteluajonPistetietoWrapper extends OptionConverter {
  def apply(pistetieto:Pistetieto):SijoitteluajonPistetietoWrapper = {
    SijoitteluajonPistetietoWrapper(
      pistetieto.getTunniste,
      convert[javaString,String](pistetieto.getArvo, string),
      convert[javaString,String](pistetieto.getLaskennallinenArvo,string),
      convert[javaString,String](pistetieto.getOsallistuminen,string)
    )
  }
}

case class SijoitteluajonHakijaryhmaWrapper(
  oid:String,
  nimi:String,
  prioriteetti:Option[Int],
  paikat:Option[Int],
  kiintio:Option[Int],
  kaytaKaikki:Option[Boolean],
  tarkkaKiintio:Option[Boolean],
  kaytetaanRyhmaanKuuluvia:Option[Boolean],
  alinHyvaksyttyPistemaara:Option[BigDecimal],
  hakemusOid:List[String]
) {
  val hakijaryhma:Hakijaryhma = {
    import scala.collection.JavaConverters._
    val hakijaryhma = new Hakijaryhma()
    hakijaryhma.setOid(oid)
    hakijaryhma.setNimi(nimi)
    prioriteetti.foreach(hakijaryhma.setPrioriteetti(_))
    paikat.foreach(hakijaryhma.setPaikat(_))
    kiintio.foreach(hakijaryhma.setKiintio(_))
    kaytaKaikki.foreach(hakijaryhma.setKaytaKaikki(_))
    tarkkaKiintio.foreach(hakijaryhma.setTarkkaKiintio(_))
    kaytetaanRyhmaanKuuluvia.foreach(hakijaryhma.setKaytetaanRyhmaanKuuluvia(_))
    alinHyvaksyttyPistemaara.foreach(pistemaara => hakijaryhma.setAlinHyvaksyttyPistemaara(pistemaara.bigDecimal))
    hakijaryhma.getHakemusOid.addAll(hakemusOid.asJava)
    hakijaryhma
  }
}

object SijoitteluajonHakijaryhmaWrapper extends OptionConverter {
  import scala.collection.JavaConverters._
  def apply(hakijaryhma:Hakijaryhma):SijoitteluajonHakijaryhmaWrapper = {
    SijoitteluajonHakijaryhmaWrapper(
      hakijaryhma.getOid,
      hakijaryhma.getNimi,
      convert[javaInt,Int](hakijaryhma.getPrioriteetti, int),
      convert[javaInt,Int](hakijaryhma.getPaikat, int),
      convert[javaInt,Int](hakijaryhma.getKiintio, int),
      convert[javaBoolean,Boolean](hakijaryhma.isKaytaKaikki, boolean),
      convert[javaBoolean,Boolean](hakijaryhma.isTarkkaKiintio, boolean),
      convert[javaBoolean,Boolean](hakijaryhma.isKaytetaanRyhmaanKuuluvia, boolean),
      convert[javaBigDecimal,BigDecimal](hakijaryhma.getAlinHyvaksyttyPistemaara, bigDecimal),
      hakijaryhma.getHakemusOid.asScala.toList
    )
  }
}

trait OptionConverter {
  def int(x:javaInt) = x.toInt
  def boolean(x:javaBoolean) = x.booleanValue
  def bigDecimal(x:javaBigDecimal) = BigDecimal(x)
  def string(x:javaString) = x

  def convert[javaType,scalaType](javaObject:javaType, f:javaType => scalaType):Option[scalaType] = javaObject match {
    case null => None //Avoid NullPointerException raised by type conversion when creating scala option with java object
    case x => Some(f(x))
  }
}
