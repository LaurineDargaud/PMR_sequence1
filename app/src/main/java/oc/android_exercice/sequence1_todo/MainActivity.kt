package oc.android_exercice.sequence1_todo

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import kotlinx.coroutines.*
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.ContextCompat.startActivity
import oc.android_exercice.sequence1_todo.data.DataProvider
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    //Initialisation des variables
    private lateinit var buttonOK: Button
    private var pseudo: EditText? = null
    private var motDePasse: EditText? = null
    var sp: SharedPreferences? = null
    private var sp_editor: SharedPreferences.Editor? = null

    var BASE_URL : String? = null

    private val activityScope = CoroutineScope(
        SupervisorJob() +
                Dispatchers.Main
    )
    var job: Job? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Gestion des SP
        sp = PreferenceManager.getDefaultSharedPreferences(this)
        sp_editor = sp?.edit()

        //Récupération des éléments graphiques du layout de l'activité dans le code
        buttonOK = findViewById(R.id.buttonOK)
        pseudo = findViewById(R.id.editTextPseudo)
        motDePasse = findViewById(R.id.editTextPassword)

        // On utilise un bundle pour détecter les déconnexions depuis les activités ChoixList et ShowList
        var bundleLogout = this.intent.extras
        var logout: Boolean = bundleLogout?.getBoolean("logout") == true
        Log.d("connexion auto", "logout state = $logout")

        //Pré-remplissage des champs pseudo et mot de passe (si on ne cherche pas à faire une déconnexion)
        val nom: String? = sp?.getString("login", "login inconnu")
        val mdp: String? = sp?.getString("mdp", "mdp inconnu")
        if (logout.not()) {
            pseudo?.setText(nom)
            motDePasse?.setText(mdp)
        }

        //Appel à la méthode gérant les clicks sur le buttonOK
        onClickFun()

        //Connexion automatique
        if (nom != "login inconnu" && mdp != "mdp inconnu" && logout.not()) {
            Log.d("connexion auto", "nom : ${nom} + mdp : ${mdp}")
            buttonOK.performClick()
        }
    }


    override fun onStart() {
        super.onStart()
    }

    //Bloquage potentiel du bouton OK
    override fun onResume() {
        super.onResume()
        //Appel de la méthode vérifiant la connexion à Internet
        if (isConnectedToInternet()) {
            buttonOK.isEnabled = true
        } else {
            buttonOK.isEnabled = false
            val internetToast: Toast = Toast.makeText(
                this,
                "Pas d'accès à Internet. Configurer votre connexion et réouvrir l'app.",
                Toast.LENGTH_LONG
            )
            internetToast.show()
        }
    }

    //Méthode gérant le clic sur le bouton OK (authentification + enregistrement des identifiants dans les SP)
    private fun onClickFun() {
        buttonOK!!.setOnClickListener {

            // Stockage du pseudo et du mdp pour une prochaine connexion
            val nom: String = pseudo?.text.toString()
            val mdp: String = motDePasse?.text.toString()
            sp_editor?.putString("login", nom)
            sp_editor?.putString("mdp", mdp)
            sp_editor?.commit()

            // Gestion de l'authentification à l'API dans une coroutine
            activityScope.launch {
                try {
                    // En cas de succès, le hash du token d'identification est enregistré dans les SP
                    // et lancement de l'activité ChoixListActivity
                    val hash = DataProvider.authentificationFromApi(nom, mdp)
                    Log.d("MainActivity login", "hash = ${hash}")
                    sp_editor?.putString("hash", hash)
                    sp_editor?.commit()
                    val intentVersChoixListActivity: Intent =
                        Intent(this@MainActivity, ChoixListActivity::class.java).apply {
                            putExtra("pseudo", nom)
                        }
                    startActivity(intentVersChoixListActivity)
                } catch (e: Exception) {
                    // L'échec de l'authentification se traduit pour l'utilisateur pour un Toast d'erreur
                    Log.d("MainActivity login", "erreur authentification = ${e}")
                    Toast.makeText(
                        this@MainActivity,
                        "Erreur d'authentification",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
            }
        }
    }

    // Fonction affichant le menu ActionBar (si la méthode renvoie vrai)
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        //Instruction pour cacher l'item déconnexion de la MainActivity (l'id de l'item est 1, l'id 0 est pour l'item "Préférences")
        menu.getItem(1).setVisible(false)
        return true
    }

    // Fonction gérant le clic sur un item du menu
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {
            // Gestion du clic l'item "Préférences"
            R.id.menu_settings -> {
                //Permet de lancer l'activité SettingsActivity
                val intentVersSettingsActivity = Intent(this, SettingsActivity::class.java)
                startActivity(intentVersSettingsActivity)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    // Fonction testant la connexion à Internet.
    fun isConnectedToInternet(): Boolean {
        val cm = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
        val isConnected: Boolean = activeNetwork?.isConnectedOrConnecting == true
        return isConnected
    }
}

