package fi.vm.sade.valintatulosservice.sijoittelu;

import java.util.Set;

public class KevytHakijaDTO {
    private String hakijaOid;
    private String hakemusOid;
    private Set<KevytHakutoiveDTO> hakutoiveet;


    public Set<KevytHakutoiveDTO> getHakutoiveet() {
        return hakutoiveet;
    }

    public void setHakutoiveet(Set<KevytHakutoiveDTO> hakutoiveet) {
        this.hakutoiveet = hakutoiveet;
    }

    public String getHakijaOid() {
        return hakijaOid;
    }

    public void setHakijaOid(String hakijaOid) {
        this.hakijaOid = hakijaOid;
    }

    public String getHakemusOid() {
        return hakemusOid;
    }

    public void setHakemusOid(String hakemusOid) {
        this.hakemusOid = hakemusOid;
    }
}
