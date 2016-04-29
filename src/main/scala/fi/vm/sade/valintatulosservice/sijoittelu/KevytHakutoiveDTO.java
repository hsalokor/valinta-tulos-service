package fi.vm.sade.valintatulosservice.sijoittelu;

import java.util.ArrayList;
import java.util.List;

public class KevytHakutoiveDTO {
    private String hakukohdeOid;
    private String tarjoajaOid;

    private List<KevytHakutoiveenValintatapajonoDTO> hakutoiveenValintatapajonot = new ArrayList<KevytHakutoiveenValintatapajonoDTO>();
    private boolean kaikkiJonotSijoiteltu = true;

    public String getHakukohdeOid() {
        return hakukohdeOid;
    }

    public void setHakukohdeOid(String hakukohdeOid) {
        this.hakukohdeOid = hakukohdeOid;
    }

    public List<KevytHakutoiveenValintatapajonoDTO> getHakutoiveenValintatapajonot() {
        return hakutoiveenValintatapajonot;
    }

    public void setHakutoiveenValintatapajonot(List<KevytHakutoiveenValintatapajonoDTO> hakutoiveenValintatapajonot) {
        this.hakutoiveenValintatapajonot = hakutoiveenValintatapajonot;
    }

    public boolean isKaikkiJonotSijoiteltu() {
        return kaikkiJonotSijoiteltu;
    }

    public void setKaikkiJonotSijoiteltu(boolean kaikkiJonotSijoiteltu) {
        this.kaikkiJonotSijoiteltu = kaikkiJonotSijoiteltu;
    }

    public String getTarjoajaOid() {
        return tarjoajaOid;
    }

    public void setTarjoajaOid(String tarjoajaOid) {
        this.tarjoajaOid = tarjoajaOid;
    }
}
