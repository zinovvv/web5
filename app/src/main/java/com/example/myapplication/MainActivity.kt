package com.example.myapplication

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

// Модель данных
data class Task(
    val id: Int? = null,
    val title: String,
    val description: String,
    val isCompleted: Boolean = false
)

// API
interface TaskApi {
    @GET("/api/tasks")
    suspend fun getTasks(): List<Task>

    @POST("/api/tasks")
    suspend fun createTask(@Body task: Task): Task

    @PUT("/api/tasks/{id}")
    suspend fun updateTask(@Path("id") id: Int, @Body task: Task): Task

    @DELETE("/api/tasks/{id}")
    suspend fun deleteTask(@Path("id") id: Int): retrofit2.Response<Void>
}

class MainActivity : AppCompatActivity() {

    private lateinit var api: TaskApi
    private lateinit var tasksContainer: LinearLayout
    private lateinit var etTitle: EditText
    private lateinit var etDescription: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализация API
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:5000")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        api = retrofit.create(TaskApi::class.java)

        // Инициализация UI
        etTitle = findViewById(R.id.etTitle)
        etDescription = findViewById(R.id.etDescription)
        tasksContainer = findViewById(R.id.tasksContainer)
        val btnAdd = findViewById<Button>(R.id.btnAdd)

        // Кнопка добавления
        btnAdd.setOnClickListener {
            createNewTask()
        }
    }

    // Этот метод вызывается каждый раз, когда мы открываем (или возвращаемся на) этот экран
    override fun onResume() {
        super.onResume()
        loadTasks()
    }

    private fun createNewTask() {
        val title = etTitle.text.toString()
        val desc = etDescription.text.toString()

        if (title.isNotEmpty()) {
            val newTask = Task(title = title, description = desc)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    api.createTask(newTask)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Задача добавлена!", Toast.LENGTH_SHORT).show()
                        etTitle.text.clear()
                        etDescription.text.clear()
                        etDescription.clearFocus()
                        loadTasks() // Перезагружаем список
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            Toast.makeText(this, "Введите название", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadTasks() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tasks = api.getTasks()
                withContext(Dispatchers.Main) {
                    tasksContainer.removeAllViews() // Очищаем список перед отрисовкой

                    for (task in tasks) {
                        val view = LayoutInflater.from(this@MainActivity)
                            .inflate(R.layout.item_task, tasksContainer, false)

                        val tvTitle = view.findViewById<TextView>(R.id.tvTaskTitle)
                        val tvDesc = view.findViewById<TextView>(R.id.tvTaskDescription)
                        val tvStatus = view.findViewById<TextView>(R.id.tvTaskStatus)
                        val btnStatus = view.findViewById<Button>(R.id.btnStatus)
                        val btnDelete = view.findViewById<Button>(R.id.btnDelete)
                        val cardView = view.findViewById<CardView>(R.id.cardViewTask)

                        tvTitle.text = task.title
                        tvDesc.text = task.description

                        // Настройка внешнего вида в зависимости от статуса
                        if (task.isCompleted) {
                            tvStatus.text = "Статус: Выполнено"
                            tvStatus.setTextColor(Color.parseColor("#065F46"))
                            cardView.setCardBackgroundColor(Color.parseColor("#D1FAE5"))
                            btnStatus.text = "Вернуть в работу"
                            btnStatus.backgroundTintList = getColorStateList(R.color.status_done) // Желтый
                        } else {
                            tvStatus.text = "Статус: В работе"
                            tvStatus.setTextColor(Color.GRAY)
                            cardView.setCardBackgroundColor(Color.WHITE)
                            btnStatus.text = "Завершить"
                            btnStatus.backgroundTintList = getColorStateList(R.color.status_done) // Желтый
                        }

                        // ЛОГИКА: СМЕНА СТАТУСА
                        btnStatus.setOnClickListener {
                            val updatedTask = task.copy(isCompleted = !task.isCompleted)
                            updateTaskStatus(task.id!!, updatedTask)
                        }

                        // ЛОГИКА: УДАЛЕНИЕ
                        btnDelete.setOnClickListener {
                            deleteTask(task.id!!)
                        }

                        // ЛОГИКА: РЕДАКТИРОВАНИЕ (Переход на другой экран)
                        view.setOnClickListener {
                            val intent = Intent(this@MainActivity, DetailActivity::class.java)
                            intent.putExtra("TASK_ID", task.id)
                            intent.putExtra("TASK_TITLE", task.title)
                            intent.putExtra("TASK_DESC", task.description)
                            intent.putExtra("TASK_COMPLETED", task.isCompleted)
                            startActivity(intent)
                        }

                        tasksContainer.addView(view)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Игнорируем ошибки при старте, если сервер выключен, чтобы не спамить
                }
            }
        }
    }

    private fun updateTaskStatus(id: Int, task: Task) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                api.updateTask(id, task)
                withContext(Dispatchers.Main) {
                    loadTasks() // Обновляем UI
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Ошибка обновления", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun deleteTask(id: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                api.deleteTask(id)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Удалено", Toast.LENGTH_SHORT).show()
                    loadTasks() // Обновляем UI
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Ошибка удаления", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}