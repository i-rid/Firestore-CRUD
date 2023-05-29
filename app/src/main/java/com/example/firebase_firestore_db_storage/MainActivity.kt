package com.example.firebase_firestore_db_storage

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.firebase_firestore_db_storage.databinding.ActivityMainBinding
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
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


        binding.btnUploadData.setOnClickListener {
            val user = getOldUserData()
            saveUser(user)
        }

        binding.btnRetrieveData.setOnClickListener {
            retrieveUserByCustomQuery()
        }

        binding.btnUpdatePerson.setOnClickListener {
            val oldUser = getOldUserData()
            val newPersonMap = getNewUserMap()
            updateUserData(oldUser, newPersonMap)
        }

        binding.btnDeletePerson.setOnClickListener {
            val person = getOldUserData()
            deleteUserData(person)
        }

        binding.btnBatchWrite.setOnClickListener {
            changeUserName("Bm8LqAXPFpqW4RbV39Rx","ChatGPT","OpenAI")
        }
    }

    private fun saveUser(user: User) = CoroutineScope(Dispatchers.IO).launch {
        var result = ""
        try{
            userCollectionRef
                .add(user)
                .addOnSuccessListener {
                    result = it.toString()
                }
                .await()

            withContext(Dispatchers.Main){
                Toast.makeText(this@MainActivity,if (result!="") result else "error",Toast.LENGTH_SHORT).show()
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

    private fun retrieveUserByCustomQuery() = CoroutineScope(Dispatchers.IO).launch {
        val fromAge = binding.etFrom.text.toString().toInt()
        val toAge   = binding.etTo.text.toString().toInt()

        try {
            val querySnapshot = userCollectionRef
                .whereGreaterThanOrEqualTo("age",fromAge)
                .whereLessThanOrEqualTo("age",toAge)
                .orderBy("age")
                .get()
                .await()
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

    private fun updateUserData(user: User, newPersonMap: Map<String,Any>)= CoroutineScope(Dispatchers.IO).launch {
        val userQuery = userCollectionRef
            .whereEqualTo("fname",user.fname)
            .whereEqualTo("lname",user.lname)
            .whereEqualTo("age",  user.age)
            .get()
            .await()

        if(userQuery.documents.isNotEmpty()){
            for (doc in userQuery){
                try {
                    //to update just one field
//                    userCollectionRef.document(doc.id).update("fname",user.fname).await()

                    userCollectionRef
                        .document(doc.id)
                        .set(newPersonMap, SetOptions.merge())
                        .await()

                }catch (e:Exception){
                    withContext(Dispatchers.Main){
                        Toast.makeText(this@MainActivity,e.message,Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        else{
            withContext(Dispatchers.Main){
                Toast.makeText(this@MainActivity,"No Match!!",Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteUserData(user: User )= CoroutineScope(Dispatchers.IO).launch{
        val userQuery = userCollectionRef
            .whereEqualTo("fname",user.fname)
            .whereEqualTo("lname",user.lname)
            .whereEqualTo("age",  user.age)
            .get()
            .await()

        if(userQuery.documents.isNotEmpty()){
            for (doc in userQuery){
                try {
                    //to delete entire document
//                    userCollectionRef
//                        .document(doc.id)
//                        .delete()
//                        .await()
                    //to delete specific field of a collection
                    userCollectionRef
                        .document(doc.id)
                        .update(
                            mapOf("fname" to FieldValue.delete())
                            )
                        .await()

                }catch (e:Exception){
                    withContext(Dispatchers.Main){
                        Toast.makeText(this@MainActivity,e.message,Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        else{
            withContext(Dispatchers.Main){
                Toast.makeText(this@MainActivity,"No Match!!",Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun changeUserName(
        userId:String,
        newFName:String,
        newLName:String
    ) = CoroutineScope(Dispatchers.IO).launch {
        try {
            Firebase
                .firestore
                .runBatch { batch ->
                    val userRefId = userCollectionRef.document(userId)
                batch.update(userRefId,"fname",newFName)
                batch.update(userRefId,"lname",newLName)
            }.await()

        }catch (e:Exception){
            withContext(Dispatchers.Main){
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    //helper classes
    private fun getOldUserData() : User{
        val fName = binding.etFirstName.text.toString()
        val lName = binding.etLastName.text.toString()
        val age = binding.etAge.text.toString().toInt()

        return User(fName, lName, age)
    }

    private fun getNewUserMap(): Map<String, Any> {
        val firstName  = binding.etNewFirstName.text.toString()
        val lastName  = binding.etNewLastName.text.toString()
        val age  = binding.etNewAge.text.toString()

        val map  = mutableMapOf<String,Any>()
        if (firstName.isNotEmpty())map["fname"]=firstName
        if (lastName.isNotEmpty())map["lname"]=lastName
        if (age.isNotEmpty())map["age"]=age.toInt()
        return map
    }

}