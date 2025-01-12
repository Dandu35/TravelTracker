package LoginyRegister

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.traveltracker.MainActivity
import com.example.traveltracker.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import model.firebase.dao.UsuarioDAO

class RegisterActivity : AppCompatActivity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val countries = resources.getStringArray(R.array.paises)
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, countries)

        val autoCompleteTextView = findViewById<AutoCompleteTextView>(R.id.autoCompleteTextViewCountry)
        autoCompleteTextView.setAdapter(adapter)

        val button = findViewById<Button>(R.id.button)
        button.setOnClickListener {
            val username = findViewById<EditText>(R.id.usernameEdit).text.toString()
            val email = findViewById<EditText>(R.id.emailEdit).text.toString()
            val password = findViewById<EditText>(R.id.passwordEdit).text.toString()
            val confirmPassword = findViewById<EditText>(R.id.confirmEdit).text.toString()
            val selectedCountry = autoCompleteTextView.text.toString()

            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || selectedCountry.isEmpty()) {
                Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Formato de correo electrónico inválido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (username.length < 6) {
                Toast.makeText(this, "El nombre de usuario debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!countries.contains(selectedCountry)) {
                Toast.makeText(this, "Seleccione un país válido de la lista", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.Main).launch {
                val usuarioDao = UsuarioDAO()

                val usuarioExiste = CoroutineScope(Dispatchers.IO).async {
                    usuarioDao.comprobarSiUsuarioExiste(username)
                }.await()

                val emailEnUso = CoroutineScope(Dispatchers.IO).async {
                    usuarioDao.comprobarSiEmailEstaEnUso(email)
                }.await()

                if (usuarioExiste) {

                    Toast.makeText(this@RegisterActivity, "Nombre de usuario no disponible", Toast.LENGTH_SHORT).show()
                    return@launch

                }

                if (emailEnUso) {

                    Toast.makeText(this@RegisterActivity, "El email ya está en uso", Toast.LENGTH_SHORT).show()
                    return@launch

                } else {
                    Log.i("Existen", usuarioExiste.toString() + emailEnUso.toString())
                    CoroutineScope(Dispatchers.Main).launch {
                        usuarioDao.insertarUsuario(username, password, email)
                    }
                    val intentRegister = Intent(this@RegisterActivity, MainActivity::class.java)
                    intentRegister.putExtra("username", username)
                    startActivity(intentRegister)

                    val intentResgister = Intent(this@RegisterActivity, MainActivity::class.java)
                    intentResgister.putExtra("username", username)


                    Toast.makeText(this@RegisterActivity, "Usuario registrado", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                    finish()
                    startActivity(intent)
                    startActivity(intentResgister)
                }
            }
        }
    }
}
