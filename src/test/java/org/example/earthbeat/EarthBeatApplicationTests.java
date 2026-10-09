package org.example.earthbeat;

import org.junit.jupiter.api.Test;
import org.example.earthjukebox.EarthJukeboxApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = EarthJukeboxApplication.class)
@ActiveProfiles("test")
class EarthBeatApplicationTests {

    @Test
    void contextLoads() {
    }

}
