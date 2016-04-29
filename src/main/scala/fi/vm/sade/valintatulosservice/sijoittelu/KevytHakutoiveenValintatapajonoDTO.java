package fi.vm.sade.valintatulosservice.sijoittelu;

import fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila;
import fi.vm.sade.sijoittelu.tulos.dto.IlmoittautumisTila;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class KevytHakutoiveenValintatapajonoDTO {
    private String valintatapajonoOid;

    private HakemuksenTila tila;
    private IlmoittautumisTila ilmoittautumisTila = IlmoittautumisTila.EI_TEHTY;

    private Integer varasijanNumero;
    private Map<String, String> tilanKuvaukset = new HashMap<String, String>();

    private boolean eiVarasijatayttoa;
    private boolean hyvaksyttyHarkinnanvaraisesti = false;
    private boolean hyvaksyttyVarasijalta;
    private boolean julkaistavissa;

    private Date valintatuloksenViimeisinMuutos;
    private Date hakemuksenTilanViimeisinMuutos;
    private Date varasijojaKaytetaanAlkaen;
    private Date varasijojaTaytetaanAsti;

    private Integer jonosija;
    private BigDecimal pisteet;

    public HakemuksenTila getTila() {
        return tila;
    }

    public void setTila(HakemuksenTila tila) {
        this.tila = tila;
    }

    public Integer getVarasijanNumero() {
        return varasijanNumero;
    }

    public void setVarasijanNumero(Integer varasijanNumero) {
        this.varasijanNumero = varasijanNumero;
    }

    public Map<String, String> getTilanKuvaukset() {
        return tilanKuvaukset;
    }

    public void setTilanKuvaukset(Map<String, String> tilanKuvaukset) {
        this.tilanKuvaukset = tilanKuvaukset;
    }

    public boolean isEiVarasijatayttoa() {
        return eiVarasijatayttoa;
    }

    public void setEiVarasijatayttoa(boolean eiVarasijatayttoa) {
        this.eiVarasijatayttoa = eiVarasijatayttoa;
    }

    public boolean isHyvaksyttyHarkinnanvaraisesti() {
        return hyvaksyttyHarkinnanvaraisesti;
    }

    public void setHyvaksyttyHarkinnanvaraisesti(boolean hyvaksyttyHarkinnanvaraisesti) {
        this.hyvaksyttyHarkinnanvaraisesti = hyvaksyttyHarkinnanvaraisesti;
    }

    public boolean isHyvaksyttyVarasijalta() {
        return hyvaksyttyVarasijalta;
    }

    public void setHyvaksyttyVarasijalta(boolean hyvaksyttyVarasijalta) {
        this.hyvaksyttyVarasijalta = hyvaksyttyVarasijalta;
    }

    public Date getHakemuksenTilanViimeisinMuutos() {
        return hakemuksenTilanViimeisinMuutos;
    }

    public void setHakemuksenTilanViimeisinMuutos(Date hakemuksenTilanViimeisinMuutos) {
        this.hakemuksenTilanViimeisinMuutos = hakemuksenTilanViimeisinMuutos;
    }

    public Date getValintatuloksenViimeisinMuutos() {
        return valintatuloksenViimeisinMuutos;
    }

    public void setValintatuloksenViimeisinMuutos(Date valintatuloksenViimeisinMuutos) {
        this.valintatuloksenViimeisinMuutos = valintatuloksenViimeisinMuutos;
    }

    public boolean isJulkaistavissa() {
        return julkaistavissa;
    }

    public void setJulkaistavissa(boolean julkaistavissa) {
        this.julkaistavissa = julkaistavissa;
    }

    public BigDecimal getPisteet() {
        return pisteet;
    }

    public void setPisteet(BigDecimal pisteet) {
        this.pisteet = pisteet;
    }

    public Integer getJonosija() {
        return jonosija;
    }

    public void setJonosija(Integer jonosija) {
        this.jonosija = jonosija;
    }

    public Date getVarasijojaKaytetaanAlkaen() {
        return varasijojaKaytetaanAlkaen;
    }

    public void setVarasijojaKaytetaanAlkaen(Date varasijojaKaytetaanAlkaen) {
        this.varasijojaKaytetaanAlkaen = varasijojaKaytetaanAlkaen;
    }

    public Date getVarasijojaTaytetaanAsti() {
        return varasijojaTaytetaanAsti;
    }

    public void setVarasijojaTaytetaanAsti(Date varasijojaTaytetaanAsti) {
        this.varasijojaTaytetaanAsti = varasijojaTaytetaanAsti;
    }

    public String getValintatapajonoOid() {
        return valintatapajonoOid;
    }

    public void setValintatapajonoOid(String valintatapajonoOid) {
        this.valintatapajonoOid = valintatapajonoOid;
    }

    public IlmoittautumisTila getIlmoittautumisTila() {
        return ilmoittautumisTila;
    }

    public void setIlmoittautumisTila(IlmoittautumisTila ilmoittautumisTila) {
        this.ilmoittautumisTila = ilmoittautumisTila;
    }
}
