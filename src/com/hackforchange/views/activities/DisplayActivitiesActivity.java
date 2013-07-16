package com.hackforchange.views.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.hackforchange.R;
import com.hackforchange.backend.activities.ActivitiesDAO;
import com.hackforchange.backend.activities.ParticipationDAO;
import com.hackforchange.backend.reminders.RemindersDAO;
import com.hackforchange.models.activities.Activities;
import com.hackforchange.models.activities.Participation;
import com.hackforchange.models.reminders.Reminders;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/*
 * Presents an activity that displays details of an existing activity
 * Also lets you edit the project (EditActivitiesActivity) or delete the project (right from this java file)
 * by choosing buttons in the ActionBar
 * Pressing the back key will exit the activity
 */
// TODO: show participation history
// TODO: participation history graph?
public class DisplayActivitiesActivity extends Activity {
  public static final String[] AllInits = {"WID", "Youth", "Malaria", "ECPA", "Food Security"};
  private ArrayList<Activities> activities_data, filteredactivities_data;
  private ActivitiesListAdapter listAdapter, tempListAdapter;
  private int activitiesid;
  private Activities a;
  private ArrayList<Reminders> reminders_data;

  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.displayactivitiesactivity);

    // read in the ID of the project that this activity must display details of
    activitiesid = getIntent().getExtras().getInt("activitiesid");
  }

  @Override
  public void onResume(){
    super.onResume();
    getActionBar().setDisplayHomeAsUpEnabled(true);
    ActivitiesDAO aDao = new ActivitiesDAO(getApplicationContext());
    a = aDao.getActivityWithId(activitiesid);
    TextView title = (TextView) findViewById(R.id.title);
    title.setText(a.getTitle());
    DateFormat parser = new SimpleDateFormat("MM/dd/yyyy");
    Date d = new Date(a.getStartDate());
    TextView startDate = (TextView) findViewById(R.id.startDate);
    startDate.setText(parser.format(d));
    d = new Date(a.getEndDate());
    TextView endDate = (TextView) findViewById(R.id.endDate);
    endDate.setText(parser.format(d));
    TextView notes = (TextView) findViewById(R.id.notes);
    notes.setText(a.getNotes());
    TextView orgs = (TextView) findViewById(R.id.orgs);
    orgs.setText(a.getOrgs());
    TextView comms = (TextView) findViewById(R.id.comms);
    comms.setText(a.getComms());
    TextView initiatives = (TextView) findViewById(R.id.initiatives);

    // convert initiatives back to human-readable form
    String[] initiativesList = a.getInitiatives().split("\\|");
    String inits = "";
    for(int i=0; i<initiativesList.length; i++){
     if(initiativesList[i].equals("1"))
       inits += AllInits[i]+"\n";
    }
    inits = (inits.length()>1)?inits.substring(0,inits.length()-1):""; // remove the last superfluous newline character
    initiatives.setText(inits);

    // display reminders
    TextView reminders = (TextView) findViewById(R.id.reminders);
    RemindersDAO rDao = new RemindersDAO(getApplicationContext());
    reminders_data = rDao.getAllRemindersForActivityId(activitiesid);
    String remindersText = "";
    Calendar c = Calendar.getInstance();
    for (Reminders r : reminders_data) {
      parser = new SimpleDateFormat("EEEE, hh:mm aaa");
      c.setTimeInMillis(r.getRemindTime());
      remindersText += parser.format(r.getRemindTime())+"\n";
    }
    remindersText = (remindersText.length()>1)?remindersText.substring(0,remindersText.length()-1):""; // remove the last superfluous newline character
    reminders.setText(remindersText);

    ArrayList <Participation> pList = new ArrayList<Participation>();
    ParticipationDAO pDao = new ParticipationDAO(getApplicationContext());
    for (Reminders r : reminders_data) {
      pList.addAll(pDao.getAllParticipationsForReminderId(r.getId()));
    }

    Button showParticipation = (Button) findViewById(R.id.showParticipation);
    // if there are no participation records associated as yet with this activity, hide the "Show Participation" button.
    // actually, we hide the linearlayout that holds it so that it takes up no space in the layout
    if(pList.size()==0){
      showParticipation.setVisibility(View.GONE);
    }
    else{
      showParticipation.setVisibility(View.VISIBLE);
      // transition to new activity that shows all the activites associated with this project
      showParticipation.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          Intent i = new Intent(DisplayActivitiesActivity.this, AllParticipationActivity.class);
          i.putExtra("activitiesid",activitiesid);
          startActivity(i);
        }
      });
    }
  }

  // create actionbar menu
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.displayactivitiesmenu, menu);
    getActionBar().setDisplayShowTitleEnabled(true);
    return true;
  }

  /*********************************************************************************************************************
   * transition to view for adding new project when the add icon in the action bar is clicked
   ********************************************************************************************************************/
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch(item.getItemId()) {
      case android.R.id.home:
        // provide a back button on the actionbar
        finish();
        break;
      case R.id.action_deleteactivity:
        // warn the user first!
        new AlertDialog.Builder(this)
          .setMessage("Are you sure you want to delete this activity? This CANNOT be undone.")
          .setCancelable(false)
          .setNegativeButton("No", null)
          .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
              ActivitiesDAO aDao = new ActivitiesDAO(getApplicationContext());
              int activityId = DisplayActivitiesActivity.this.activitiesid;
              aDao.deleteActivities(activityId);
              // cancel all alarms for participation events of the reminders of this activity
              RemindersDAO rDao = new RemindersDAO(getApplicationContext());
              ArrayList<Reminders> reminders_data;
              reminders_data = rDao.getAllRemindersForActivityId(activityId);
              for(Reminders r: reminders_data){
                EditActivitiesActivity.deleteAlarmsForReminder(getApplicationContext(),r.getId());
              }
              finish();
            }
          })
          .show();
        break;
      case R.id.action_editactivity:
        Intent i = new Intent(DisplayActivitiesActivity.this, EditActivitiesActivity.class);
        i.putExtra("activitiesid",activitiesid);
        startActivity(i);
        break;
      default:
        return super.onOptionsItemSelected(item);
    }

    return true;
  }
}