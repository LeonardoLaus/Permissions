package cherry.android.permissions;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class SecondFragmentActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second_fragment);
        getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, new SecondFragment()).commit();
    }
}
