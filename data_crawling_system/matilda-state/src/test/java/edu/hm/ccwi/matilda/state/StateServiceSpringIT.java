package edu.hm.ccwi.matilda.state;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestContext.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class StateServiceSpringIT {

    @Test
    public void startSpringContext() {
        // DO NOTHING - Just start spring context
    }
}
