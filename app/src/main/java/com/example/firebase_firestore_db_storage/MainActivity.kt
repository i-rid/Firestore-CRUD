package com.example.firebase_firestore_db_storage

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.firebase_firestore_db_storage.databinding.ActivityMainBinding
import com.google.firebase.firestore.ktx.firestore
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
}