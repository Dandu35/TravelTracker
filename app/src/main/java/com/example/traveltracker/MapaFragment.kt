package com.example.traveltracker

import Mapa.CountryAdapter
import Mapa.Country
import Mapa.MapaHash
import VisitedCountriesAdapter
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SearchView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import controlers.GuardarFotoController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import model.local.database.LocalDatabase
import model.local.entity.EstadoPais
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * Fragmento que muestra un mapa interactivo con información sobre países.
 */
class MapaFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CountryAdapter
    private lateinit var countries: List<Country>
    private lateinit var searchView: SearchView
    private var selectedCountryId: String? = null
    private lateinit var button: Button
    private lateinit var buttonVerificar: Button
    private var contadorVistados: Int = 0
    private var contadorConfirmados:Int = 0
    private val countriesVisitados = mutableListOf<Country>()
    var colorPais: Int = android.R.color.transparent
    private lateinit var visitedCountriesAdapter: VisitedCountriesAdapter
    val mapasHash = hashMapOf<String, MapaHash>(
        "NL" to MapaHash("#3DDC84", "@string/Paises_Bajos", "M844,301.1h3v6c-0.7,2.3 -1,4.6 -1,7 -5.5,0.6 -8.9,-1.8 -10,-7 1.8,-3.2 4.5,-5.2 8,-6Z"),
        "ES" to MapaHash("#727473", "@string/España", "M778,403.1c-0.5,-6.4 0.8,-12.4 4,-18 0.2,-2.4 -0.4,-4.4 -2,-6 -3.1,-1 -6.5,-1.3 -10,-1v-6c-0,-2 1,-3 3,-3 3,0.9 6,1.8 9,2.5 8,0.3 16,0.7 24,1 6.3,4.2 13.3,6.3 21,6.5 -9.8,5.5 -15.6,13.8 -17.5,25 -6.9,5.7 -14.9,9 -24,10 -2,-0.2 -3.8,-0.9 -5.5,-2 -0.9,-3 -1.5,-6 -2,-9Z")
    )

    /**
     * Método llamado cuando el fragmento está siendo creado.
     *
     * @param savedInstanceState Si no es nulo, este fragmento está siendo reconstruido a partir de un estado guardado.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

        CoroutineScope(Dispatchers.Main).launch {
            val miDatabase = LocalDatabase.getInstance(requireContext())
            val estadoPaisDAO = miDatabase.estadoPaisDao()

            val paisesVisitados: List<EstadoPais> = withContext(Dispatchers.IO) {
                estadoPaisDAO.getPaisesVisitados()
            }

            for (pais in paisesVisitados) {
                countriesVisitados.add(getCountryById(pais.nombrePais))
                Log.i("País añadido", "El país ${pais.codPais} se ha añadido.")
            }

            contadorVistados = paisesVisitados.count().toInt()
            val textView = view?.findViewById<TextView>(R.id.numPaisesVisitados)
            if (textView != null) {
                textView.text = contadorVistados.toString()
            }
            visitedCountriesAdapter.notifyDataSetChanged()
        }
    }

    /**
     * Infla el diseño del fragmento y configura las interacciones de los elementos de la interfaz de usuario.
     *
     * @param inflater El inflador utilizado para inflar el diseño.
     * @param container El contenedor padre en el que se debe colocar el diseño inflado.
     * @param savedInstanceState El estado previamente guardado del fragmento.
     * @return La vista inflada para el fragmento o nulo si se produce un error.
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_mapa, container, false)

        button = view.findViewById(R.id.button6)
        button.setOnClickListener {
            mapasHash[selectedCountryId]?.color = "#3DDC84"
            crearXML(requireContext())


                    paisesVisitados(selectedCountryId, countries)
                    val miDatabase = LocalDatabase.getInstance(requireContext())
                    val estadoPaisDAO = miDatabase.estadoPaisDao()

                    CoroutineScope(Dispatchers.Main).launch {
                        estadoPaisDAO.updateHaEstado(selectedCountryId.toString(), true)
                    }
                    colorPais = R.color.visitado
                    contadorVistados += 1
                    val textView = view.findViewById<TextView>(R.id.numPaisesVisitados)
                    textView.text = contadorVistados.toString()
                    Log.d( "contador", "$contadorVistados")


        }
        button = view.findViewById(R.id.button5)
        button.setOnClickListener {


            val miDatabase = LocalDatabase.getInstance(requireContext())
            val estadoPaisDAO = miDatabase.estadoPaisDao()
            CoroutineScope(Dispatchers.Main).launch {
                estadoPaisDAO.updateHaEstado(selectedCountryId.toString(), false)
            }
            eliminarPaisesVisitados(selectedCountryId, countries)
            if (contadorVistados == 0) {
                contadorVistados = 0
            }else{
                contadorVistados -= 1
            }

            val textView = view.findViewById<TextView>(R.id.numPaisesVisitados)
            textView.text = contadorVistados.toString()
        }



        buttonVerificar = view.findViewById(R.id.button7)
        buttonVerificar.setOnClickListener {
            val gfc = GuardarFotoController(requireContext())

            gfc.getPhotoAndSaveOnDb()
            contadorConfirmados += 1
            val textView = view.findViewById<TextView>(R.id.numPaisesConfirmados)
            textView.text = contadorConfirmados.toString()
        }
        recyclerView = view.findViewById(R.id.paco)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())


        countries = listOf(
            Country("Afganistán ", "\uD83C\uDDE6\uD83C\uDDEB","AF"),
            Country("Albania ", "\uD83C\uDDE6\uD83C\uDDF1", "AL"),
            Country("Argelia ", "\uD83C\uDDE9\uD83C\uDDFF", "DZ"),
            Country("Andorra ", "\uD83C\uDDE6\uD83C\uDDE9", "AD"),
            Country("Angola ", "\uD83C\uDDE6\uD83C\uDDF4", "AO"),
            Country("Antigua y Barbuda ", "\uD83C\uDDE6\uD83C\uDDEC","AG"),
            Country("Argentina ", "\uD83C\uDDE6\uD83C\uDDF7", "AR"),
            Country("Armenia ", "\uD83C\uDDE6\uD83C\uDDF2", "AM"),
            Country("Australia ", "\uD83C\uDDE6\uD83C\uDDFA", "AU"),
            Country("Austria ", "\uD83C\uDDE6\uD83C\uDDF9", "AT"),
            Country("Bahamas ", "\uD83C\uDDE7\uD83C\uDDF8", "BS"),
            Country("Baréin ", "\uD83C\uDDE7\uD83C\uDDEA", "BH"),
            Country("Bangladés ", "\uD83C\uDDE7\uD83C\uDDE9", "BD"),
            Country("Barbados ", "\uD83C\uDDE7\uD83C\uDDE7", "BB"),
            Country("Bielorrusia ", "\uD83C\uDDE7\uD83C\uDDFE", "BY"),
            Country("Bélgica ", "\uD83C\uDDE7\uD83C\uDDED", "BE"),
            Country("Belice ", "\uD83C\uDDE7\uD83C\uDDFF", "BZ"),
            Country("Benín ", "\uD83C\uDDE7\uD83C\uDDEF", "BJ"),
            Country("Bután ", "\uD83C\uDDE7\uD83C\uDDF9", "BT"),
            Country("Bolivia ", "\uD83C\uDDE7\uD83C\uDDF4", "BO"),
            Country("Bosnia y Herzegovina ", "\uD83C\uDDE7\uD83C\uDDEE", "BA"),
            Country("Botsuana ", "\uD83C\uDDE7\uD83C\uDDFC", "BW"),
            Country("Brasil ", "\uD83C\uDDE7\uD83C\uDDF7", "BR"),
            Country("Brunéi ", "\uD83C\uDDE7\uD83C\uDDF3", "BN"),
            Country("Bulgaria ", "\uD83C\uDDE7\uD83C\uDDEC", "BG"),
            Country("Burkina Faso ", "\uD83C\uDDE7\uD83C\uDDEB", "BF"),
            Country("Burundi ", "\uD83C\uDDE7\uD83C\uDDEC", "BI"),
            Country("Cabo Verde ", "\uD83C\uDDE8\uD83C\uDDFB", "CV"),
            Country("Camboya ", "\uD83C\uDDF0\uD83C\uDDED", "KH"),
            Country("Camerún ", "\uD83C\uDDE8\uD83C\uDDF2", "CM"),
            Country("Chad ", "\uD83C\uDDE8\uD83C\uDDEC", "TD"),
            Country("Chile ", "\uD83C\uDDE8\uD83C\uDDF1", "CL"),
            Country("China ", "\uD83C\uDDE8\uD83C\uDDF3", "CN"),
            Country("Colombia ", "\uD83C\uDDE8\uD83C\uDDF4", "CO"),
            Country("Comoras ", "\uD83C\uDDE8\uD83C\uDDF2", "KM"),
            Country("Congo ", "\uD83C\uDDE8\uD83C\uDDEE", "CG"),
            Country("Costa Rica ", "\uD83C\uDDE8\uD83C\uDDF7", "CR"),
            Country("Croacia ", "\uD83C\uDDE8\uD83C\uDDF7", "HR"),
            Country("Cuba ", "\uD83C\uDDE8\uD83C\uDDFA", "Cu"),
            Country("Chipre ", "\uD83C\uDDE8\uD83C\uDDFE", "CY"),
            Country("República Checa ", "\uD83C\uDDE8\uD83C\uDDEC", "CZ"),
            Country("Dinamarca ", "\uD83C\uDDE9\uD83C\uDDF0", "DK"),
            Country("Yibuti ", "\uD83C\uDDE9\uD83C\uDDEE", "DJ"),
            Country("Dominica ", "\uD83C\uDDE9\uD83C\uDDF2", "DM"),
            Country("República Dominicana ", "\uD83C\uDDE9\uD83C\uDDF4", "DO"),
            Country("Ecuador ", "\uD83C\uDDEA\uD83C\uDDE8", "EC"),
            Country("Egipto ", "\uD83C\uDDEA\uD83C\uDDEC", "EG"),
            Country("El Salvador ", "\uD83C\uDDEA\uD83C\uDDF8", "SV"),
            Country("Guinea Ecuatorial ", "\uD83C\uDDEC\uD83C\uDDEA", "GQ"),
            Country("Eritrea ", "\uD83C\uDDEA\uD83C\uDDF7", "ER"),
            Country("Estonia ", "\uD83C\uDDEA\uD83C\uDDEA", "EE"),
            Country("Eswatini ", "\uD83C\uDDEA\uD83C\uDDFC", "SZ"),
            Country("Etiopía ", "\uD83C\uDDEA\uD83C\uDDF9", "ET"),
            Country("Fiyi ", "\uD83C\uDDEB\uD83C\uDDF3", "FJ"),
            Country("Finlandia ", "\uD83C\uDDEB\uD83C\uDDEE", "FI"),
            Country("Gabón ", "\uD83C\uDDEC\uD83C\uDDE6", "GA"),
            Country("Gambia ", "\uD83C\uDDEC\uD83C\uDDF2", "GM"),
            Country("Georgia ", "\uD83C\uDDEC\uD83C\uDDEA", "GE"),
            Country("Ghana ", "\uD83C\uDDEC\uD83C\uDDED", "GH"),
            Country("Grecia ", "\uD83C\uDDEC\uD83C\uDDF7", "GR"),
            Country("Granada ", "\uD83C\uDDEC\uD83C\uDDF9", "GD"),
            Country("Guatemala ", "\uD83C\uDDEC\uD83C\uDDF9", "GT"),
            Country("Guinea ", "\uD83C\uDDEC\uD83C\uDDF3", "GN"),
            Country("Guinea-Bisáu ", "\uD83C\uDDEC\uD83C\uDDEE", "GW"),
            Country("Guyana ", "\uD83C\uDDEC\uD83C\uDDFE", "GY"),
            Country("Haití ", "\uD83C\uDDED\uD83C\uDDF9", "HT"),
            Country("Honduras ", "\uD83C\uDDED\uD83C\uDDF3", "HN"),
            Country("Hungría ", "\uD83C\uDDED\uD83C\uDDFA", "HU"),
            Country("Islandia ", "\uD83C\uDDEE\uD83C\uDDF8", "IS"),
            Country("India ", "\uD83C\uDDEE\uD83C\uDDF3", "IN"),
            Country("Indonesia ", "\uD83C\uDDEE\uD83C\uDDE9", "ID"),
            Country("Irán ", "\uD83C\uDDEE\uD83C\uDDF7", "IR"),
            Country("Iraq ", "\uD83C\uDDEE\uD83C\uDDF6", "IQ"),
            Country("Irlanda ", "\uD83C\uDDEE\uD83C\uDDEA", "IE"),
            Country("Israel ", "\uD83C\uDDEE\uD83C\uDDF1", "IL"),
            Country("Costa de Marfil ", "\uD83C\uDDEE\uD83C\uDDE9", "CI"),
            Country("Jamaica ", "\uD83C\uDDEF\uD83C\uDDF2", "JM"),
            Country("Japón ", "\uD83C\uDDEF\uD83C\uDDF5", "JP"),
            Country("Jordania ", "\uD83C\uDDEF\uD83C\uDDF5", "JO"),
            Country("Kazajistán ", "\uD83C\uDDF0\uD83C\uDDFF", "KZ"),
            Country("Kenia ", "\uD83C\uDDF0\uD83C\uDDEA", "KE"),
            Country("Kiribati ", "\uD83C\uDDF0\uD83C\uDDEE", "KI"),
            Country("Kuwait ", "\uD83C\uDDF0\uD83C\uDDFC", "KW"),
            Country("Kirguistán ", "\uD83C\uDDF0\uD83C\uDDEC", "KG"),
            Country("Laos ", "\uD83C\uDDF1\uD83C\uDDE6", "LA"),
            Country("Letonia ", "\uD83C\uDDF1\uD83C\uDDED", "LV"),
            Country("Líbano ", "\uD83C\uDDF1\uD83C\uDDE7", "LB"),
            Country("Lesoto ", "\uD83C\uDDF1\uD83C\uDDE7", "LS"),
            Country("Liberia ", "\uD83C\uDDF1\uD83C\uDDE7", "LR"),
            Country("Libia ", "\uD83C\uDDF1\uD83C\uDDEE", "LY"),
            Country("Liechtenstein ", "\uD83C\uDDF1\uD83C\uDDEE", "LI"),
            Country("Lituania ", "\uD83C\uDDF1\uD83C\uDDF9", "LT"),
            Country("Luxemburgo ", "\uD83C\uDDF1\uD83C\uDDF8", "LU"),
            Country("Macedonia del Norte ", "\uD83C\uDDF2\uD83C\uDDE6", "MK"),
            Country("Madagascar ", "\uD83C\uDDF2\uD83C\uDDEC", "MG"),
            Country("Malaui ", "\uD83C\uDDF2\uD83C\uDDFC", "MW"),
            Country("Malasia ", "\uD83C\uDDF2\uD83C\uDDFE", "MY"),
            Country("Maldivas ", "\uD83C\uDDF2\uD83C\uDDFB", "MV"),
            Country("Malí ", "\uD83C\uDDF2\uD83C\uDDF1", "ML"),
            Country("Malta ", "\uD83C\uDDF2\uD83C\uDDF9", "MT"),
            Country("Islas Marshall ", "\uD83C\uDDF2\uD83C\uDDF5", "MH"),
            Country("Mauritania ", "\uD83C\uDDF2\uD83C\uDDF7", "MR"),
            Country("México ", "\uD83C\uDDF2\uD83C\uDDFD", "MX"),
            Country("Micronesia ", "\uD83C\uDDEB\uD83C\uDDEE", "FM"),
            Country("Moldavia ", "\uD83C\uDDF2\uD83C\uDDE9", "MD"),
            Country("Mónaco ", "\uD83C\uDDF2\uD83C\uDDE8","MC"),
            Country("Mongolia ", "\uD83C\uDDF2\uD83C\uDDF3", "MN"),
            Country("Montenegro ", "\uD83C\uDDF2\uD83C\uDDEA" , "ME"),
            Country("Marruecos ", "\uD83C\uDDF2\uD83C\uDDEC" , "MA"),
            Country("Mozambique ", "\uD83C\uDDF2\uD83C\uDDFF" , "MZ"),
            Country("Myanmar ", "\uD83C\uDDF2\uD83C\uDDF2", "MM"),
            Country("Namibia ", "\uD83C\uDDF3\uD83C\uDDE6", "NA"),
            Country("Nauru ", "\uD83C\uDDF3\uD83C\uDDF7", "NR"),
            Country("Nepal ", "\uD83C\uDDF3\uD83C\uDDF5", "NP"),
            Country("Países Bajos ", "\uD83C\uDDF3\uD83C\uDDF1", "NL"),
            Country("Nueva Zelanda ", "\uD83C\uDDF3\uD83C\uDDFF", "NZ"),
            Country("Nicaragua ", "\uD83C\uDDEB\uD83C\uDDF2", "NI"),
            Country("Níger ", "\uD83C\uDDF3\uD83C\uDDEC", "NE"),
            Country("Nigeria ", "\uD83C\uDDF3\uD83C\uDDEC", "NG"),
            Country("Noruega ", "\uD83C\uDDF3\uD83C\uDDF4", "NO"),
            Country("Omán ", "\uD83C\uDDF4\uD83C\uDDF2", "OM"),
            Country("Pakistán ", "\uD83C\uDDF5\uD83C\uDDE6", "PK"),
            Country("Palaos ", "\uD83C\uDDF5\uD83C\uDDE6", "PW"),
            Country("Panamá ", "\uD83C\uDDF5\uD83C\uDDE6", "PA"),
            Country("Papúa Nueva Guinea ", "\uD83C\uDDF5\uD83C\uDDEC","PG"),
            Country("Paraguay ", "\uD83C\uDDF5\uD83C\uDDFE", "PY"),
            Country("Perú ", "\uD83C\uDDF5\uD83C\uDDEA", "PE"),
            Country("Filipinas ", "\uD83C\uDDF5\uD83C\uDDED", "PH"),
            Country("Polonia ", "\uD83C\uDDF5\uD83C\uDDF1", "PL"),
            Country("Portugal ", "\uD83C\uDDF5\uD83C\uDDF9", "PT"),
            Country("Catar ", "\uD83C\uDDF6\uD83C\uDDE6", "QA"),
            Country("Rumania ", "\uD83C\uDDF7\uD83C\uDDF4", "RO"),
            Country("Rusia ", "\uD83C\uDDF7\uD83C\uDDFA", "RU"),
            Country("Ruanda ", "\uD83C\uDDF7\uD83C\uDDFC", "RW"),
            Country("San Cristóbal y Nieves ", "\uD83C\uDDF0\uD83C\uDDF3", "KN"),
            Country("Santa Lucía ", "\uD83C\uDDF1\uD83C\uDDE8", "LC"),
            Country("Alemania ", "\uD83C\uDDE9\uD83C\uDDEA", "DE"),
            Country("Arabia Saudita ", "\uD83C\uDDF8\uD83C\uDDE6", "SA"),
            Country("Bielorrusia ", "\uD83C\uDDE7\uD83C\uDDFE", "BY"),
            Country("Birmania ", "\uD83C\uDDF2\uD83C\uDDF2" , "MM"),
            Country("Canadá ", "\uD83C\uDDE8\uD83C\uDDE6", "CA"),
            Country("Ciudad del Vaticano ", "\uD83C\uDDFB\uD83C\uDDE6", "VA"),
            Country("Corea del Norte ", "\uD83C\uDDF0\uD83C\uDDF5", "KP"),
            Country("Corea del Sur ", "\uD83C\uDDF0\uD83C\uDDF7", "KR"),
            Country("Emiratos Árabes Unidos ", "\uD83C\uDDE6\uD83C\uDDEA", "AE"),
            Country("Eslovaquia ", "\uD83C\uDDF8\uD83C\uDDF0", "SK"),
            Country("Eslovenia ", "\uD83C\uDDF8\uD83C\uDDEE", "SI"),
            Country("Estados Unidos", "\uD83C\uDDFA\uD83C\uDDF8", "US"),
            Country("Francia ", "\uD83C\uDDEB\uD83C\uDDF7", "FR"),
            Country("Irak ", "\uD83C\uDDEE\uD83C\uDDF6", "IQ"),
            Country("Italia ", "\uD83C\uDDEE\uD83C\uDDF9", "IT"),
            Country("Reino Unido ", "\uD83C\uDDEC\uD83C\uDDE7", "GB"),
            Country("República del Congo ", "\uD83C\uDDE8\uD83C\uDDE9", "CG"),
            Country("República Democrática del Congo ", "\uD83C\uDDE8\uD83C\uDDE9", "CD"),
            Country("República Sudafricana ", "\uD83C\uDDFF\uD83C\uDDE6", "ZA"),
            Country("Samoa ", "\uD83C\uDDFC\uD83C\uDDF8", "WS"),
            Country("San Marino ", "\uD83C\uDDF8\uD83C\uDDF2", "SM"),
            Country("San Vicente y las Granadinas ", "\uD83C\uDDFB\uD83C\uDDE8", "VC"),
            Country("Santo Tomé y Príncipe ", "\uD83C\uDDF8\uD83C\uDDF9", "ST"),
            Country("Serbia ", "\uD83C\uDDF7\uD83C\uDDF8", "SR"),
            Country("Seychelles ", "\uD83C\uDDF8\uD83C\uDDE8", "SC"),
            Country("Singapur ", "\uD83C\uDDF8\uD83C\uDDEC", "SG"),
            Country("Somalia ", "\uD83C\uDDF8\uD83C\uDDF4", "SO"),
            Country("Sri Lanka ", "\uD83C\uDDF1\uD83C\uDDF0", "LK"),
            Country("Suazilandia ", "\uD83C\uDDF8\uD83C\uDDFF", "SZ"),
            Country("Sudán ", "\uD83C\uDDF8\uD83C\uDDE9", "SD"),
            Country("Sudán del Sur ", "\uD83C\uDDF8\uD83C\uDDF8", "SS"),
            Country("Suecia ", "\uD83C\uDDF8\uD83C\uDDEA", "SE"),
            Country("Suiza ", "\uD83C\uDDE8\uD83C\uDDED", "CH"),
            Country("Surinam ", "\uD83C\uDDF8\uD83C\uDDF7", "SR"),
            Country("Tailandia ", "\uD83C\uDDF9\uD83C\uDDED", "TH"),
            Country("Tanzania ", "\uD83C\uDDF9\uD83C\uDDFF", "TZ"),
            Country("Timor Oriental ", "\uD83C\uDDF9\uD83C\uDDF4", "TL"),
            Country("Togo ", "\uD83C\uDDF9\uD83C\uDDEC", "TG"),
            Country("Tonga ", "\uD83C\uDDF9\uD83C\uDDF4", "TO"),
            Country("Trinidad y Tobago ", "\uD83C\uDDF9\uD83C\uDDF9", "TT"),
            Country("Turkmenistán ", "\uD83C\uDDF9\uD83C\uDDF2", "TM"),
            Country("Tuvalu ", "\uD83C\uDDF9\uD83C\uDDFB", "TV"),
            Country("Ucrania ", "\uD83C\uDDFA\uD83C\uDDE6", "UA"),
            Country("Uganda ", "\uD83C\uDDFA\uD83C\uDDEC", "UG"),
            Country("Uruguay ", "\uD83C\uDDFA\uD83C\uDDFE", "UY"),
            Country("Uzbekistán ", "\uD83C\uDDFA\uD83C\uDDFF", "UZ"),
            Country("Vanuatu ", "\uD83C\uDDFB\uD83C\uDDFA", "VU"),
            Country("Venezuela ", "\uD83C\uDDFB\uD83C\uDDEA", "VE"),
            Country("Vietnam ", "\uD83C\uDDFB\uD83C\uDDF3", "VN"),
            Country("Yemen ", "\uD83C\uDDFE\uD83C\uDDEA", "YE"),
            Country("Zambia ", "\uD83C\uDDFF\uD83C\uDDF2", "ZM"),
            Country("Zimbabue ", "\uD83C\uDDFF\uD83C\uDDFC", "ZW"),
            Country("Kosovo ", "\uD83C\uDDFD\uD83C\uDDF0", "XK"),
            Country("Taiwán ", "\uD83C\uDDF9\uD83C\uDDFC", "TW"),
            Country("Sahara Occidental ", "\uD83C\uDDEA\uD83C\uDDF8", "EH"),
            Country("Santa Sede ", "\uD83C\uDDFB\uD83C\uDDE6", "VA"),
            Country("Palestina ", "\uD83C\uDDF5\uD83C\uDDF8", "PS"),
            Country("España ", "\uD83C\uDDEA\uD83C\uDDF8", "ES"),
        )

        val visitedCountriesRecyclerView: RecyclerView = view.findViewById(R.id.imageView4)
        visitedCountriesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        visitedCountriesAdapter = VisitedCountriesAdapter(countriesVisitados, { countryId ->
            selectedCountryId = countryId
            Log.d("MapaFragment", "Selected Country ID: $selectedCountryId") // Registro en Logcat
        }, this) // Pasa una instancia válida de MapaFragment al adaptador

        visitedCountriesRecyclerView.adapter = visitedCountriesAdapter

        adapter = CountryAdapter(countries){ countryId ->
            selectedCountryId = countryId
            Log.d("MapaFragment", "Selected Country ID: $selectedCountryId") // Registro en Logcat
        }
        recyclerView.adapter = adapter



        searchView = view.findViewById(R.id.searchView)
        searchView.queryHint = "Busca un país...";

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.filter.filter(newText)

                recyclerView.visibility = if (newText.isNullOrEmpty()) View.GONE else View.VISIBLE

                return false
            }

        })

        return view
    }

    /**
     * Obtiene un país según su ID.
     *
     * @param id El ID del país a buscar.
     * @return El país correspondiente al ID proporcionado.
     * @throws NoSuchElementException Si no se encuentra un país con el ID especificado.
     */
    fun getCountryById(id: String): Country {
        return countries.find { it.id == id }!!
    }

    /**
     * Agrega un país a la lista de países visitados.
     *
     * @param selectedCountryID El ID del país seleccionado.
     * @param countries La lista de todos los países disponibles.
     */
    fun paisesVisitados(selectedcountryID: String?, countries: List<Country>) {
        selectedcountryID?.let { id ->
            for (country in countries) {
                if (country.id == id) {
                    countriesVisitados.add(country)
                    break
                }
            }
        }


        visitedCountriesAdapter.notifyDataSetChanged()

        for (visitedCountry in countriesVisitados) {
            Log.d(
                "PaisesVisitados",
                "ID: ${visitedCountry.id}, Nombre: ${visitedCountry.name}, Emoji de bandera: ${visitedCountry.flagEmoji}"
            )
        }
    }

    /**
     * Elimina un país de la lista de países visitados.
     *
     * @param selectedCountryID El ID del país seleccionado.
     * @param countries La lista de todos los países disponibles.
     */
    fun eliminarPaisesVisitados(selectedcountryID: String?, countries: List<Country>) {
        selectedcountryID?.let { id ->
            for (country in countries) {
                if (country.id == id) {
                    countriesVisitados.remove(country)
                    break
                }
            }
        }

        visitedCountriesAdapter.notifyDataSetChanged()

        for (visitedCountry in countriesVisitados) {
            Log.d(
                "PaisesVisitados",
                "ID: ${visitedCountry.id}, Nombre: ${visitedCountry.name}, Emoji de bandera: ${visitedCountry.flagEmoji}"
            )
        }
    }

    /**
     * Crea un archivo XML con la información de los países y lo guarda en la memoria interna.
     *
     * @param context El contexto de la aplicación.
     */
    fun crearXML(context: Context) {
        val xmlBuilder = StringBuilder("<vector xmlns:android=\"http://schemas.android.com/apk/res/android\" android:width=\"1726.7799dp\" android:height=\"960dp\"\n" +
                "    android:viewportWidth=\"1726.6\" android:viewportHeight=\"959.9\">\n")

        for ((clave, valor) in mapasHash) {
            xmlBuilder.append("    <path android:id=\"@+id/${clave}\" android:fillColor=\"${valor.color}\" android:fillType=\"evenOdd\" android:name=\"${valor.nombre}\"\n" +
                    "        android:pathData=\"${valor.data}\" android:strokeWidth=\"0\"/>\n")
        }

        xmlBuilder.append("</vector>")

        val xmlString = xmlBuilder.toString()

        val filename = "mapa2d_nuevo_test.xml"
        val fileContents = xmlString.toByteArray()
        val file = File(context.filesDir, filename)

        if (file.exists()) {
            file.delete()
        }

        FileOutputStream(file).use {
            it.write(fileContents)
        }

        if (file.exists()) {
            Log.d("XML_GUARDADO", "El archivo XML se ha guardado en la memoria interna en: ${file.absolutePath}")

            val xmlContent = file.readText()
            Log.d("XML_CONTENIDO", "Contenido del archivo XML:\n$xmlContent")
        } else {
            Log.e("ERROR", "No se pudo guardar el archivo XML en la memoria interna")
        }
    }

    /**
     * Muestra el contenido de un archivo XML en un TextView.
     *
     * @param context El contexto de la aplicación.
     * @param textView El TextView donde se mostrará el contenido del archivo XML.
     */
    fun mostrarXMLenTextView(context: Context, textView: TextView) {
        val filename = "mapa2d_nuevo_test.xml"
        val file = File(context.filesDir, filename)

        if (file.exists()) {
            try {
                val xmlContent = file.readText()
                textView.text = xmlContent
                Log.d("XML_MOSTRADO", "El contenido del archivo XML se ha mostrado en el TextView")
            } catch (e: IOException) {
                e.printStackTrace()
                Log.e("ERROR", "No se pudo leer el contenido del archivo XML")
            }
        } else {
            Log.e("ERROR", "El archivo XML no existe en la memoria interna")
        }
    }

}
