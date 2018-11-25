package ohtu.verkkokauppa;

import org.junit.*;
import static org.mockito.Mockito.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

public class KauppaTest {
    // luodaan ensin mock-oliot
    Pankki pankki = mock(Pankki.class);
    Viitegeneraattori mockViite = mock(Viitegeneraattori.class);
    Varasto varasto = mock(Varasto.class);
    Kauppa kauppa;
    
    @Test
    public void poistaKoristaToimii() {
        kauppa = new Kauppa(varasto, pankki, mockViite); 
        
        when(varasto.saldo(1)).thenReturn(10); 
        when(varasto.saldo(2)).thenReturn(10); 
        when(varasto.haeTuote(1)).thenReturn(new Tuote(1, "limu", 6));
        when(varasto.haeTuote(2)).thenReturn(new Tuote(2, "sipsi", 4));
        
        kauppa.aloitaAsiointi();
        kauppa.lisaaKoriin(1);
        kauppa.lisaaKoriin(1);
        kauppa.poistaKorista(1);
        kauppa.lisaaKoriin(2);
        kauppa.lisaaKoriin(2);
        kauppa.lisaaKoriin(2);
        kauppa.poistaKorista(2);
        kauppa.tilimaksu("petteri", "9999");
        
        verify(pankki).tilisiirto(anyString(), anyInt(), anyString(), anyString(),eq(14));
    }
    
    @Test
    public void pyydetaanUusiViiteJokaiseenMaksuun() {
        kauppa = new Kauppa(varasto, pankki, mockViite); 

        kauppa.aloitaAsiointi();
        kauppa.lisaaKoriin(5);
        kauppa.tilimaksu("somebody", "1111");

        // tarkistetaan että tässä vaiheessa viitegeneraattorin metodia seuraava()
        // on kutsuttu kerran
        verify(mockViite, times(1)).uusi();

        kauppa.aloitaAsiointi();
        kauppa.lisaaKoriin(5);
        kauppa.tilimaksu("somebodyElse", "2222");

        // tarkistetaan että tässä vaiheessa viitegeneraattorin metodia seuraava()
        // on kutsuttu kaksi kertaa
        verify(mockViite, times(2)).uusi();

        kauppa.aloitaAsiointi();
        kauppa.lisaaKoriin(5);
        kauppa.tilimaksu("whoever", "3333");

        // tarkistetaan että tässä vaiheessa viitegeneraattorin metodia seuraava()
        // on kutsuttu kolme kertaa        
        verify(mockViite, times(3)).uusi();
    }
    
    @Test
    public void aloitaAsiointiNollaaEdellisenOstoksenTiedot() {
        kauppa = new Kauppa(varasto, pankki, mockViite); 
        
        when(varasto.saldo(1)).thenReturn(10); 
        when(varasto.saldo(2)).thenReturn(10); 
        when(varasto.haeTuote(1)).thenReturn(new Tuote(1, "bisse", 6));
        when(varasto.haeTuote(2)).thenReturn(new Tuote(1, "karkkipussi", 4));
        
        kauppa.aloitaAsiointi();
        kauppa.lisaaKoriin(1);
        kauppa.lisaaKoriin(2);
        kauppa.tilimaksu("petteri", "3153150");
        
        kauppa.aloitaAsiointi();
        kauppa.lisaaKoriin(2);
        kauppa.tilimaksu("jaska", "1337");
        
        verify(pankki).tilisiirto(eq("jaska"), anyInt(), eq("1337"), anyString(),eq(4));
    }
   
    @Test
    public void asiointiSujuuKunToinenTuoteOnLoppu() {
        kauppa = new Kauppa(varasto, pankki, mockViite); 
        
//        when(viite.uusi()).thenReturn(42);
        when(varasto.saldo(1)).thenReturn(10); 
        when(varasto.saldo(2)).thenReturn(0); 
        when(varasto.haeTuote(1)).thenReturn(new Tuote(1, "bisse", 6));
        when(varasto.haeTuote(2)).thenReturn(new Tuote(1, "karkkipussi", 4));
        
        
        kauppa.aloitaAsiointi();
        kauppa.lisaaKoriin(1);
        kauppa.lisaaKoriin(2);
        kauppa.tilimaksu("petteri", "3153150");
        
        verify(pankki).tilisiirto(eq("petteri"), anyInt(), eq("3153150"), anyString(),eq(6));
    }
    
    @Test
    public void asiointiSujuuKahdellaSamallaTuotteella() {
        kauppa = new Kauppa(varasto, pankki, mockViite); 
        
//        when(viite.uusi()).thenReturn(42);
        when(varasto.saldo(1)).thenReturn(10); 
        when(varasto.haeTuote(1)).thenReturn(new Tuote(1, "bisse", 6));
        
        
        kauppa.aloitaAsiointi();
        kauppa.lisaaKoriin(1);
        kauppa.lisaaKoriin(1);
        kauppa.tilimaksu("arska", "715517");
        
        verify(pankki).tilisiirto(eq("arska"), anyInt(), eq("715517"), anyString(),eq(12));
    }
    
    @Test
    public void asiointiSujuuKahdellaEriTuotteella() {
        kauppa = new Kauppa(varasto, pankki, mockViite); 
        
//        when(viite.uusi()).thenReturn(42);
        when(varasto.saldo(1)).thenReturn(10); 
        when(varasto.saldo(2)).thenReturn(5); 
        when(varasto.haeTuote(1)).thenReturn(new Tuote(1, "maito", 5));
        when(varasto.haeTuote(2)).thenReturn(new Tuote(2, "leipa", 3));
        
        
        kauppa.aloitaAsiointi();
        kauppa.lisaaKoriin(1);
        kauppa.lisaaKoriin(2);
        kauppa.tilimaksu("eero", "54321");
        
        verify(pankki).tilisiirto(eq("eero"), anyInt(), eq("54321"), anyString(),eq(8));
    }
    
    @Test
    public void ostoksenPaaytyttyaPankinMetodiaTilisiirtoKutsutaan() {
        // määritellään että viitegeneraattori palauttaa viitten 42
        when(mockViite.uusi()).thenReturn(42);

        
        // määritellään että tuote numero 1 on maito jonka hinta on 5 ja saldo 10
        when(varasto.saldo(1)).thenReturn(10); 
        when(varasto.haeTuote(1)).thenReturn(new Tuote(1, "maito", 5));

        // sitten testattava kauppa 
        Kauppa k = new Kauppa(varasto, pankki, mockViite);              

        // tehdään ostokset
        k.aloitaAsiointi();
        k.lisaaKoriin(1);     // ostetaan tuotetta numero 1 eli maitoa
        k.tilimaksu("pekka", "12345");

        // sitten suoritetaan varmistus, että pankin metodia tilisiirto on kutsuttu
        verify(pankki).tilisiirto(eq("pekka"), anyInt(), eq("12345"), anyString(),anyInt());   
        // toistaiseksi ei välitetty kutsussa käytetyistä parametreista
    }
}
