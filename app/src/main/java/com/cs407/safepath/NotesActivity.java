package com.cs407.safepath;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class NotesActivity extends AppCompatActivity {

    TextView textView1;
    public static ArrayList<Note> notes1 = new ArrayList<Note>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        Button returnButton = findViewById(R.id.notesButton);
        returnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                returnToMainActivity();
            }
        });

        textView1 =(TextView) findViewById(R.id.textView1);

        Context context = getApplicationContext();
        DBHelper dbHelper = new DBHelper(context);

        notes1 = dbHelper.getAllNotes();
        ArrayList<String> displayNotes = new ArrayList<>();
        for (Note notes: notes1) {
            displayNotes.add(String.format("From: %s\nTo: %s\nDate & Time: %s\nDistance: %s", notes.getAddress(), notes.getDestination(), notes.getDate(), notes.getDistance()));
        }
        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, displayNotes);
        ListView notesListView = (ListView) findViewById(R.id.listView1);
        notesListView.setAdapter(adapter);



    }

    public void returnToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

}