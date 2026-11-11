package com.jarvis.assistant.finishpolish
import android.content.Intent
import android.os.Bundle
import androidx.compose.material3.*
import com.jarvis.assistant.omnisystem.PrivateModuleActivity
class FinishPolishActivity:PrivateModuleActivity(){
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);if(isFinishing)return
  screen("JARVIS · INTELLIGENCE & SYSTEMS"){
   Text("Your existing assistant, with evidence-based diagnostics and optional personal routines. Saved voices, models, pairing and privacy controls remain in place.")
   Button(onClick={startActivity(Intent(this@FinishPolishActivity,com.jarvis.assistant.finishpolish.doctor.SystemDoctorActivity::class.java))}){Text("System Doctor · check, review, repair and verify")}
   Button(onClick={startActivity(Intent(this@FinishPolishActivity,com.jarvis.assistant.finishpolish.learning.LearningActivity::class.java))}){Text("Teach Jarvis · approved phrases and recipes")}
   Button(onClick={startActivity(Intent(this@FinishPolishActivity,com.jarvis.assistant.finishpolish.predictive.PredictiveActivity::class.java))}){Text("Future Actions · optional suggestions and routines")}
   Button(onClick={startActivity(Intent(this@FinishPolishActivity,com.jarvis.assistant.finishpolish.mission.MissionControlActivity::class.java))}){Text("Mission Control · paired computer and verified jobs")}
   Text("No background conversations are training data. Suggestions never execute an external action by themselves. Review any recipe before running. Android permissions and device authentication remain under your control.")
  }
 }
}
