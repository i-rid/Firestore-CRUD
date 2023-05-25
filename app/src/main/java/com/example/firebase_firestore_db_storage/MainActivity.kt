package com.example.firebase_firestore_db_storage

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.firebase_firestore_db_storage.databinding.ActivityMainBinding
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding:ActivityMainBinding
    private val userCollectionRef = Firebase.firestore.collection("users_")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnUploadData.setOnClickListener {
            val fName = binding.etFirstName.text.toString()
            val lName = binding.etLastName.text.toString()
            val age = binding.etAge.text.toString().toInt()

            saveUser(User(fName, lName, age))
        }

//        binding.btnRetrieveData.setOnClickListener {
//            retrieveUser()
//        }
        updateDataInRealTime()

    }

    private fun saveUser(user: User) = CoroutineScope(Dispatchers.IO).launch {
        var result:String = ""
        try{
            userCollectionRef
                .add(user)
                .addOnSuccessListener {
                    result = it.toString()
                }
                .await()

            withContext(Dispatchers.Main){
                Toast.makeText(this@MainActivity,if (result!="")result.toString() else "error",Toast.LENGTH_SHORT).show()
            }
        }catch (e:Exception){
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
            }

        }
    }
    private fun retrieveUser() = CoroutineScope(Dispatchers.IO).launch {
        try {
            val querySnapshot = userCollectionRef.get().await()
            val stringBuilder = StringBuilder()
            for(doc in querySnapshot.documents){
                val user = doc.toObject<User>()
                stringBuilder.append("$user\n")
            }
            withContext(Dispatchers.Main){
                binding.tvPersons.text = stringBuilder.toString()
            }
        }catch (e: Exception){
            withContext(Dispatchers.Main){
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateDataInRealTime(){
        userCollectionRef.addSnapshotListener { querySnapshot, firebaseException ->
            firebaseException?.let {
                Toast.makeText(this@MainActivity,firebaseException.message,Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }

            querySnapshot?.let {
                    val stringBuilder = StringBuilder()
                    for(doc in it){
                        val user = doc.toObject<User>()
                        stringBuilder.append("$user\n")
                    }
                    binding.tvPersons.text = stringBuilder.toString()
            }
        }
    }
}