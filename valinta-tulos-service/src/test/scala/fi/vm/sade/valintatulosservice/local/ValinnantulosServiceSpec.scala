package fi.vm.sade.valintatulosservice.local

import java.time.{Instant, ZonedDateTime}

import fi.vm.sade.valintatulosservice.{ValinnantulosService, ValinnantulosUpdateStatus}
import fi.vm.sade.valintatulosservice.security.{CasSession, Role, ServiceTicket}
import fi.vm.sade.valintatulosservice.valintarekisteri.db.ValinnantulosRepository
import fi.vm.sade.valintatulosservice.valintarekisteri.domain._
import org.junit.runner.RunWith
import org.specs2.matcher.MustThrownExpectations
import org.specs2.mock.Mockito
import org.specs2.mock.mockito.{MockitoMatchers, MockitoStubs}
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import org.specs2.specification.Scope
import slick.dbio.{DBIO, DBIOAction}

import scala.concurrent.duration.Duration

@RunWith(classOf[JUnitRunner])
class ValinnantulosServiceSpec extends Specification with MockitoMatchers with MockitoStubs {
  val tarjoajaOid = "1.2.3.4.5"
  val valintatapajonoOid = "14538080612623056182813241345174"
  val hakemusOids = List("1.2.246.562.11.00006169120", "1.2.246.562.11.00006169121", "1.2.246.562.11.00006169122",
    "1.2.246.562.11.00006169123", "1.2.246.562.11.00006169124", "1.2.246.562.11.00006169125")
  lazy val valinnantulos = Valinnantulos(
    hakukohdeOid = "1.2.246.562.20.26643418986",
    valintatapajonoOid = valintatapajonoOid,
    hakemusOid = hakemusOids(0),
    henkiloOid = "1.2.246.562.24.48294633106",
    valinnantila = Hylatty,
    ehdollisestiHyvaksyttavissa = false,
    julkaistavissa = false,
    hyvaksyttyVarasijalta = false,
    hyvaksyPeruuntunut = false,
    vastaanottotila = Poista,
    ilmoittautumistila = EiTehty)
  val session = CasSession(ServiceTicket("myFakeTicket"), "123.123.123", Set(Role.SIJOITTELU_CRUD))

  "ValinnantulosService" in {
    "status is 404 if valinnantulos is not found" in new ValinnantulosServiceWithMocks {
      override def result = List()
      val valinnantulokset = List(valinnantulos, valinnantulos.copy(hakemusOid = s"${hakemusOids(1)}"))
      service.storeValinnantuloksetAndIlmoittautumiset(valintatapajonoOid, valinnantulokset, ZonedDateTime.now.toInstant, session) mustEqual
        List(ValinnantulosUpdateStatus(404, s"Not found", valintatapajonoOid, hakemusOids(0)), ValinnantulosUpdateStatus(404, s"Not found", valintatapajonoOid, hakemusOids(1)))
    }
    "status is 409 if valinnantulos has been modified" in new ValinnantulosServiceWithMocks {
      override def result = List((ZonedDateTime.now.toInstant, valinnantulos))
      val notModifiedSince = ZonedDateTime.now.minusDays(2).toInstant
      val valinnantulokset = List(valinnantulos.copy(julkaistavissa = true))
      service.storeValinnantuloksetAndIlmoittautumiset(valintatapajonoOid, valinnantulokset, notModifiedSince, session) mustEqual
        List(ValinnantulosUpdateStatus(409, s"Not unmodified since ${notModifiedSince}", valintatapajonoOid, hakemusOids(0)))
    }
    "no status for unmodified valinnantulos" in new ValinnantulosServiceWithMocks {
      override def result = List((ZonedDateTime.now.toInstant, valinnantulos))
      val valinnantulokset = List(valinnantulos)
      service.storeValinnantuloksetAndIlmoittautumiset(valintatapajonoOid, valinnantulokset, ZonedDateTime.now.toInstant, session) mustEqual List()
    }
    "no status for succesfully modified valinnantulos" in new ValinnantulosServiceWithMocks {
      override def result = List((ZonedDateTime.now.toInstant, valinnantulos))
      val valinnantulokset = List(valinnantulos.copy(julkaistavissa = true))
      service.storeValinnantuloksetAndIlmoittautumiset(valintatapajonoOid, valinnantulokset, ZonedDateTime.now.toInstant, session) mustEqual List()
    }
    "different statuses for all failing valinnantulokset" in new ValinnantulosServiceWithMocks {
      override def result = List(
        (ZonedDateTime.now.toInstant, valinnantulos),
        (ZonedDateTime.now.toInstant, valinnantulos.copy(hakemusOid = hakemusOids(1))),
        (ZonedDateTime.now.toInstant, valinnantulos.copy(hakemusOid = hakemusOids(2), julkaistavissa = true)),
        (ZonedDateTime.now.toInstant, valinnantulos.copy(hakemusOid = hakemusOids(3))),
        (ZonedDateTime.now.toInstant, valinnantulos.copy(hakemusOid = hakemusOids(4))),
        (ZonedDateTime.now.toInstant, valinnantulos.copy(hakemusOid = hakemusOids(5)))
      )
      val valinnantulokset = List(
        valinnantulos.copy(valinnantila = Hyvaksytty),
        valinnantulos.copy(hakemusOid = hakemusOids(1), ehdollisestiHyvaksyttavissa = true),
        valinnantulos.copy(hakemusOid = hakemusOids(2), vastaanottotila = VastaanotaSitovasti),
        valinnantulos.copy(hakemusOid = hakemusOids(3), hyvaksyttyVarasijalta = true),
        valinnantulos.copy(hakemusOid = hakemusOids(4), hyvaksyPeruuntunut = true),
        valinnantulos.copy(hakemusOid = hakemusOids(5), ilmoittautumistila = Lasna)
      )
      service.storeValinnantuloksetAndIlmoittautumiset(valintatapajonoOid, valinnantulokset, ZonedDateTime.now.toInstant, session) mustEqual List(
        ValinnantulosUpdateStatus(403, s"Valinnantilan muutos ei ole sallittu", valintatapajonoOid, valinnantulokset(0).hakemusOid),
        ValinnantulosUpdateStatus(403, s"Ehdollisesti hyväksyttävissä -arvon muuttaminen ei ole sallittua", valintatapajonoOid, valinnantulokset(1).hakemusOid),
        ValinnantulosUpdateStatus(403, s"Julkaistavissa-arvon muuttaminen ei ole sallittua", valintatapajonoOid, valinnantulokset(2).hakemusOid),
        ValinnantulosUpdateStatus(403, s"Hyväksytty varasijalta -arvon muuttaminen ei ole sallittua", valintatapajonoOid, valinnantulokset(3).hakemusOid),
        ValinnantulosUpdateStatus(403, s"HyväksyPeruuntunut value cannot be changed", valintatapajonoOid, valinnantulokset(4).hakemusOid),
        ValinnantulosUpdateStatus(403, s"Ilmoittautumista ei voida muuttaa", valintatapajonoOid, valinnantulokset(5).hakemusOid)
      )
    }
  }

  trait ValinnantulosServiceWithMocks extends Mockito with Scope with MustThrownExpectations {
    def result:List[(Instant, Valinnantulos)]

    val valinnantulosRepository = mock[ValinnantulosRepository]
    val service = new ValinnantulosService(valinnantulosRepository)

    valinnantulosRepository.getTarjoajaForHakukohde(anyString) returns tarjoajaOid
    valinnantulosRepository.getValinnantuloksetForValintatapajono(valintatapajonoOid) returns result
  }
}