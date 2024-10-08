package findnest.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void init() {
        try {
            // Get Firebase credentials from the environment variable
            String firebaseConfig = System.getenv("GOOGLE_CREDENTIALS");

            if (firebaseConfig == null) {
                throw new IOException("GOOGLE_CREDENTIALS environment variable is not set.");
            }

            // Convert the environment variable string into an InputStream
            InputStream serviceAccount = new ByteArrayInputStream(firebaseConfig.getBytes());

            // Firebase options
            FirebaseOptions options = new FirebaseOptions.Builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .setDatabaseUrl("https://findnest-capstone-default-rtdb.firebaseio.com/")
                .setStorageBucket("findnest-54a57.appspot.com")
                .build();

            // Initialize Firebase app only if it's not already initialized
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                System.out.println("Firebase application initialized.");
            } else {
                System.out.println("Firebase application already initialized.");
            }
        } catch (IOException e) {
            System.err.println("Error initializing Firebase: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
