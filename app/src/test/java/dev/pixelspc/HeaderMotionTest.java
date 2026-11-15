package dev.pixelspc;
import org.junit.Test;
import static org.junit.Assert.*;

public class HeaderMotionTest {
    @Test public void hifiMarqueePausesExitsAndReturnsWithoutClippingTheCycle(){
        assertEquals(0,ChipLogo.subtitleOffset(0,180,300,20),0);
        assertEquals(0,ChipLogo.subtitleOffset(4499,180,300,20),0);
        assertEquals(-90,ChipLogo.subtitleOffset(9000,180,300,20),.01f);
        assertEquals(-180,ChipLogo.subtitleOffset(13499,180,300,20),.01f);
        assertEquals(300,ChipLogo.subtitleOffset(13500,180,300,20),.01f);
        assertEquals(150,ChipLogo.subtitleOffset(21000,180,300,20),.01f);
        assertEquals(0,ChipLogo.subtitleOffset(28500,180,300,20),.01f);
    }
    @Test public void invalidOrNotYetStartedMotionStaysAtRest(){
        assertEquals(0,ChipLogo.subtitleOffset(-100,180,300,20),0);
        assertEquals(0,ChipLogo.subtitleOffset(9000,0,300,20),0);
        assertEquals(0,ChipLogo.subtitleOffset(9000,180,300,0),0);
    }
}
