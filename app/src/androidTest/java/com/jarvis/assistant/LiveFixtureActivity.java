package com.jarvis.assistant;

public class LiveFixtureActivity extends android.app.Activity {
    protected void onCreate(android.os.Bundle state) {
        super.onCreate(state);
        getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        android.widget.EditText draft = new android.widget.EditText(this);
        draft.setHint("Draft"); draft.setText("Original draft");
        android.widget.Button search = new android.widget.Button(this);
        search.setText("Search");
        search.setOnClickListener(v -> { search.setText("Clicked"); draft.requestFocus(); });
        layout.addView(search); layout.addView(draft);
        android.widget.Button send = new android.widget.Button(this); send.setText("Send"); layout.addView(send);
        setContentView(layout); draft.requestFocus();
    }
}
