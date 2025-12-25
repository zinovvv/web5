package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class DetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        // Получаем данные, переданные с главного экрана
        val taskId = intent.getIntExtra("TASK_ID", -1)
        val taskTitle = intent.getStringExtra("TASK_TITLE") ?: ""
        val taskDesc = intent.getStringExtra("TASK_DESC") ?: ""
        val taskCompleted = intent.getBooleanExtra("TASK_COMPLETED", false)

        val etTitle = findViewById<EditText>(R.id.etEditTitle)
        val etDesc = findViewById<EditText>(R.id.etEditDescription)
        val btnSave = findViewById<Button>(R.id.btnSave)

        // Заполняем поля текущими данными
        etTitle.setText(taskTitle)
        etDesc.setText(taskDesc)

        // Настройка API
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:5000")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api = retrofit.create(TaskApi::class.java)

        btnSave.setOnClickListener {
            val newTitle = etTitle.text.toString()
            val newDesc = etDesc.text.toString()

            val updatedTask = Task(
                id = taskId,
                title = newTitle,
                description = newDesc,
                isCompleted = taskCompleted
            )

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    api.updateTask(taskId, updatedTask)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(applicationContext, "Сохранено!", Toast.LENGTH_SHORT).show()
                        finish() // Закрываем экран и возвращаемся назад
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(applicationContext, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}