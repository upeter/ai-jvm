package dev.example

import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.web.client.RestTemplate
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlow
import org.springframework.web.reactive.function.client.exchangeToFlow

class ChatApiTest {
    val webClient = WebClient.builder().build()
    val restTemplate = RestTemplate()

    @Test
    fun `call blocking get request with rest template`() {
        val messageChatRequest = ChatRequest(
            message = "Translate the following csv from italian to english: {Crema di zucca,Primi piatti,\"[['Zucca', '1kg'], ['Patate', '200g'], ['Brodo vegetale', '1l'], ['Cipolle bianche', '80g'], ['Pepe nero', '1pizzico'], ['Sale', '1pizzico'], [\"\"Olio extravergine d'oliva\"\", '60g'], ['Cannella in polvere', '1pizzico'], ['Noce moscata', '1pizzico'], [\"\"Olio extravergine d'oliva\"\", '30g'], ['Pane casereccio', '100g']]\"}",
            system = null
        )
        val systemChatRequest = ChatRequest(
            message = """Translate the following from italian to english, keeping the exact same format: {$data}""",
            system = null//"You are a translator that translates italian to english. Return the translation in the received format like csv, json, xml etc."
        )
        println(restTemplate.sendChatRequest(systemChatRequest))
    }

    @Test
    fun `call streaming get request with web client`() = runBlocking {
        val systemChatRequest = ChatRequest(
            message = """Translate the following from italian to english, keeping the exact same format: {$data}""",
            system = null//"You are a translator that translates italian to english. Return the translation in the received format like csv, json, xml etc."
        )
        webClient.sendStreamingChatRequest(systemChatRequest).collect {
            println(it) // prints the response line by line

        }
    }
}

fun RestTemplate.sendChatRequest(chatRequest: ChatRequest): Map<String, String> {
    val url = "http://localhost:8080/ai"
    return this.postForObject(url, chatRequest, Map::class.java) as Map<String, String>
}

fun WebClient.sendStreamingChatRequest(chatRequest: ChatRequest): Flow<String> {
    val url = "http://localhost:8080/ai/stream"
    return this.post()
        .uri(url)
        .bodyValue(chatRequest)
        .retrieve()
        .bodyToFlow<String>()
}


val data =
    """Chili con carne,Secondi piatti,"[['Manzo', '800g'], ['Fagioli neri precotti', '700g'], ['Peperoni rossi', '250g'], ['Passata di pomodoro', '500g'], ['Brodo di carne', '500g'], ['Cipolle bianche', '160g'], ['Cipolle rosse', '100g'], ['Aglio', '3'], ['Peperoncino fresco', '1'], ['Cumino in polvere', '1cucchiaio'], ['Coriandolo in polvere', '1cucchiaio'], ['Zucchero di canna', '1cucchiaio'], [""Olio extravergine d'oliva"", '30g'], ['Sale fino', '2'], ['Pepe nero', '1cucchiaio'], ['Coriandolo', 'q.b.']]"
Insalata di finocchi e arance,Insalate,"[['Finocchi', '840g'], ['Arance', '2'], ['Pinoli', '50g'], ['Uvetta', '20g'], ['Sale', 'q.b.'], ['Olio di oliva', '50g'], ['Aceto', '3g'], ['Semi di zucca', '8g']]"
Pasta e patate alla napoletana,Primi piatti,"[['Pasta Mista', '320g'], ['Patate', '750g'], ['Sedano', '150g'], ['Carote', '150g'], ['Cipolle bianche', '1'], ['Lardo', '130g'], ['Concentrato di pomodoro', '20g'], ['Rosmarino', '1'], ['Parmigiano Reggiano DOP', '1'], [""Olio extravergine d'oliva"", 'q.b.'], ['Sale fino', 'q.b.'], ['Pepe nero', 'q.b.']]"
Crocchette di patate,Antipasti,"[['Patate rosse', '1kg'], ['Tuorli', '30g'], ['Noce moscata', 'q.b.'], ['Pepe nero', 'q.b.'], ['Sale fino', 'q.b.'], ['Parmigiano Reggiano DOP', '100g'], ['Uova', '130g'], ['Pangrattato', 'q.b.'], ['Olio di semi di arachide', 'q.b.']]"
Panettone gastronomico,Antipasti,"[['Farina Manitoba', '150g'], ['Latte intero', '100g'], ['Lievito di birra secco', '2g'], ['Farina 0', '400g'], ['Latte intero', '120g'], ['Burro', '80g'], ['Zucchero', '40g'], ['Uova', '1'], ['Tuorli', '1'], ['Lievito di birra secco', '2g'], ['Sale fino', '8g'], ['Malto', '2g'], ['Formaggio fresco spalmabile', '100g'], ['Panna fresca liquida', '10g'], ['Sale fino', 'q.b.'], ['Pepe nero', 'q.b.'], ['Erba cipollina', '15g'], ['Salmone affumicato', '80g'], ['Asparagi', '150g'], ['Robiola', '100g'], ['Sale fino', 'q.b.'], ['Pepe nero', 'q.b.'], ['Caprino', '160g'], ['Concentrato di pomodoro', '10g'], ['Panna fresca liquida', '20g'], ['Radicchio', '40g'], ['Prosciutto cotto', '100g'], ['Pepe nero', 'q.b.'], ['Sale fino', 'q.b.'], ['Gamberi', '170g'], ['Erba cipollina', '5g'], ['Salsa cocktail', '80g']]"
Pasta e lenticchie,Primi piatti,"[['Ditaloni Rigati', '350g'], ['Lenticchie', '200g'], ['Pancetta affumicata', '80g'], ['Passata di pomodoro', '100g'], ['Carote', '80g'], ['Cipolle', '80g'], ['Sedano', '60g'], ['Brodo vegetale', '1l'], ['Aglio fresco', '1'], ['Rosmarino', '1'], ['Timo', '1'], ['Parmigiano Reggiano DOP', '40g'], ['Peperoncino', '1g'], [""Olio extravergine d'oliva"", '30g'], ['Sale fino', 'q.b.'], ['Pepe nero', 'q.b.']]"
Cosce di pollo al forno,Secondi piatti,"[['Cosce di pollo', '430g'], ['Patate', '500g'], ['Sale fino', 'q.b.'], ['Pepe nero', 'q.b.'], [""Olio extravergine d'oliva"", '50g'], ['Rosmarino', '3'], ['Timo', '3'], ['Paprika piccante', '2']]"
Dorayaki,Dolci,"[['Acqua', '180g'], ['Farina 00', '240g'], ['Zucchero a velo', '150g'], ['Uova', '2'], ['Lievito in polvere per dolci', '3g'], ['Miele', '20g'], ['Olio di semi', 'q.b.']]"
Polpette di zucca,Antipasti,"[['Zucca', '500g'], ['Pangrattato', '100g'], ['Parmigiano Reggiano DOP', '100g'], ['Scamorza affumicata', '50g'], ['Uova', '1'], ['Salvia', 'q.b.'], [""Olio extravergine d'oliva"", 'q.b.'], ['Sale fino', 'q.b.'], ['Pepe nero', 'q.b.'], ['Pangrattato', 'q.b.'], ['Olio di semi', 'q.b.']]"
Crostata alla confettura di albicocche,Dolci,"[['Farina 00', '250g'], ['Burro', '150g'], ['Zucchero a velo', '100g'], ['Tuorli', '80g'], ['Miele di acacia', '20g'], ['Scorza di limone', '½'], ['Baccello di vaniglia', '½'], ['Sale fino', '1g'], ['Confettura di albicocche', '250g'], ['Scorza di limone', '½']]"
Risotto ai funghi porcini,Primi piatti,"[['Riso Carnaroli', '320g'], ['Funghi porcini', '400g'], ['Brodo vegetale', '1l'], ['Cipolle dorate', '1'], ['Aglio', '1'], ['Burro', '30g'], [""Olio extravergine d'oliva"", '2'], ['Sale fino', 'q.b.'], ['Pepe nero', 'q.b.'], ['Parmigiano Reggiano DOP', '50g'], ['Burro', '30g'], ['Prezzemolo', '2']]"
Spaghetti alla puttanesca,Primi piatti,"[['Spaghetti', '320g'], ['Pomodori pelati', '800g'], [""Acciughe sott'olio"", '25g'], ['Capperi sotto sale', '10g'], ['Prezzemolo', '1'], ['Olive di Gaeta', '100g'], ['Aglio', '3'], ['Peperoncino secco', '2'], [""Olio extravergine d'oliva"", '30g'], ['Sale fino', 'q.b.']]"
Salmone al forno,Secondi piatti,"[['Tranci di salmone', '660g'], ['Patate', '170g'], ['Scorza di limone', '1'], ['Succo di limone', '25g'], ['Vino bianco', '25g'], [""Olio extravergine d'oliva"", '50g'], ['Prezzemolo', '1cucchiaio'], ['Sale fino', 'q.b.'], ['Pepe nero', 'q.b.']]"
Piadina Romagnola,Piatti Unici,"[['Farina 00', '500g'], ['Strutto', '125g'], ['Acqua', '170g'], ['Sale fino', '15g'], ['Bicarbonato', '5']]"
Pasta con la zucca,Primi piatti,"[['Farfalle', '320g'], ['Zucca', '600g'], ['Pancetta affumicata', '60g'], ['Rosmarino', '1'], ['Brodo vegetale', '150g'], [""Olio extravergine d'oliva"", '50g'], ['Ricotta vaccina', '40g'], ['Scalogno', '30g'], ['Sale fino', 'q.b.'], ['Pepe nero', 'q.b.']]"
Insalata di mare,Antipasti,"[['Cozze', '1kg'], ['Vongole', '750g'], ['Polpo', '700g'], ['Gamberi', '500g'], ['Calamari', '400g'], ['Carote', '2'], ['Sedano', '2'], ['Aglio', '1'], ['Alloro', '4'], ['Prezzemolo', 'q.b.'], ['Pepe nero in grani', 'q.b.'], ['Sale grosso', 'q.b.'], ['Succo di limone', '40g'], [""Olio extravergine d'oliva"", '40g'], ['Prezzemolo', 'q.b.'], ['Sale fino', 'q.b.'], ['Pepe nero', 'q.b.']]"
Arrosto di vitello al forno con patate,Secondi piatti,"[['Sottofesa di vitello', '600g'], ['Patate', '1kg'], ['Vino bianco', '50g'], ['Rosmarino', '2'], ['Aglio', '2'], [""Olio extravergine d'oliva"", '40g'], ['Sale fino', 'q.b.'], ['Pepe nero', 'q.b.']]"
Polpettone al forno,Secondi piatti,"[['Manzo', '600'], ['Salsiccia', '400g'], ['Pane', '200g'], ['Pecorino', '150g'], ['Latte intero', '200g'], ['Uova', '110g'], ['Timo', '3'], ['Noce moscata', '½'], ['Sale fino', 'q.b.'], ['Pepe nero', 'q.b.'], [""Olio extravergine d'oliva"", 'q.b.'], ['Patate rosse', '500g'], ['Scalogno', '2'], ['Salvia', 'qualchefoglia'], ['Timo', '3'], ['Sale fino', 'q.b.'], ['Pepe nero', 'q.b.'], [""Olio extravergine d'oliva"", 'q.b.']]"
Pizzette di sfoglia,Antipasti,"[['Olive nere', '4'], [""Capperi sott'aceto"", '20'], ['Provola', '65g'], ['Uova', '1'], ['Origano', 'mezzocucchiaio'], ['Pasta Sfoglia', '230g'], [""Acciughe sott'olio"", '5'], ['Passata di pomodoro', '50g'], ['Pesto alla Genovese', '3']]"
Scaloppine ai funghi,Secondi piatti,"[['Vitello', '400g'], ['Funghi champignon', '500g'], ['Burro', '50g'], ['Pepe nero', 'q.b.'], ['Farina 00', '40g'], ['Aglio', '1'], [""Olio extravergine d'oliva"", '10g'], ['Sale fino', 'q.b.'], ['Timo', 'q.b.'], ['Rosmarino', '1']]"
Torta al latte caldo,Dolci,"[['Latte intero', '200g'], ['Uova', '210g'], ['Farina 00', '200g'], ['Zucchero', '180g'], ['Burro', '60g'], ['Lievito in polvere per dolci', '6g'], ['Scorza di limone', '1'], ['Sale fino', '1pizzico']]"
Pasta alla Norma,Primi piatti,"[['Sedani Rigati', '500g'], ['Melanzane', '1'], ['Ricotta di pecora', '150g'], ['Sale fino', 'q.b.'], ['Pomodori costoluti', '5kg'], ['Aglio', '4'], ['Sale fino', 'q.b.'], ['Basilico', '1'], [""Olio extravergine d'oliva"", 'q.b.']]"
Croccante alle mandorle,Dolci,"[['Mandorle pelate', '500g'], ['Zucchero', '350g'], ['Miele di acacia', '125g'], ['Succo di limone', 'qualchegoccia']]"
Mozzarella in carrozza,Antipasti,"[['Pane bianco in cassetta', '12'], ['Mozzarella di bufala', '500g'], ['Sale fino', 'q.b.'], ['Uova', '5'], ['Farina 00', '100g'], ['Pangrattato', '300g'], ['Olio di semi di girasole', '1l']]"
Lingue di gatto,Dolci,"[['Burro', '50g'], ['Zucchero a velo', '60g'], ['Albumi', '50g'], ['Farina 0', '50g']]"
Risotto ai funghi,Primi piatti,"[['Riso Carnaroli', '240g'], ['Funghi chiodini', '200g'], ['Funghi champignon', '200g'], ['Cipolle', '½'], ['Burro', '80g'], ['Parmigiano Reggiano DOP', '60g'], ['Sale fino', 'q.b.'], ['Prezzemolo', 'q.b.'], ['Vino bianco', '50g'], ['Acqua', '1l'], ['Pepe bianco', 'q.b.'], [""Olio extravergine d'oliva"", 'q.b.']]"
Baccalà mantecato alla veneziana,Antipasti,"[['Stoccafisso', '500g'], [""Olio extravergine d'oliva"", '280g'], ['Limoni', '½'], ['Aglio', '1'], ['Alloro', '2'], ['Sale fino', 'q.b.'], ['Pepe nero', 'q.b.'], ['Prezzemolo', 'q.b.'], [""Olio extravergine d'oliva"", 'q.b.']]"
Mandorle pralinate,Dolci,"[['Mandorle', '150g'], ['Zucchero', '120g'], ['Acqua', '35g']]"
Zuppa di pesce,Primi piatti,"[['Gamberi', '6'], ['Seppie', '400g'], ['Gallinella', '550g'], ['Coda di rospo', '500g'], ['Triglie', '550g'], [""Olio extravergine d'oliva"", 'q.b.'], ['Aglio', '2'], ['Prezzemolo', 'q.b.'], ['Vino bianco', '50g'], ['Sale fino', 'q.b.'], ['Pepe nero', 'q.b.'], ['Passata di pomodoro', '500g'], ['Sedano', '1'], ['Carote', '1'], ['Cipolle bianche', '1'], ['Acqua', '5l'], ['Pepe bianco in grani', 'q.b.'], ['Prezzemolo', 'q.b.'], ['Cozze', '1kg'], [""Olio extravergine d'oliva"", 'q.b.'], ['Aglio', '1']]"
Polpette di spinaci e ricotta,Secondi piatti,"[['Spinaci', '250g'], ['Ricotta vaccina', '250g'], ['Grana Padano DOP', '50g'], ['Pangrattato', '40g'], [""Olio extravergine d'oliva"", '20g'], ['Aglio', '1'], ['Sale fino', 'q.b.'], ['Pepe nero', 'q.b.'], ['Uova', '1'], ['Pangrattato', 'q.b.'], ['Sale fino', 'q.b.'], ['Pepe nero', 'q.b.']]"
Pasta e fagioli,Primi piatti,"[['Ditaloni Rigati', '320g'], ['Fagioli borlotti secchi', '200g'], ['Passata di pomodoro', '250g'], ['Lardo', '80g'], ['Prosciutto crudo', '80g'], ['Cipolle', '30g'], ['Sedano', '30g'], ['Carote', '30g'], ['Aglio', '1'], ['Rosmarino', '3'], ['Alloro', '2'], [""Olio extravergine d'oliva"", '10g'], ['Pepe nero', 'q.b.'], ['Sale fino', 'q.b.']]"
Pizza Margherita,Lievitati,"[['Farina Manitoba', '200g'], ['Farina 00', '300g'], ['Acqua', '300ml'], ['Sale fino', '10g'], ['Lievito di birra fresco', '4g'], ['Passata di pomodoro', '300g'], ['Mozzarella', '200g'], ['Basilico', 'q.b.'], [""Olio extravergine d'oliva"", 'q.b.'], ['Semola di grano duro rimacinata', 'q.b.']]"
Cheesecake al limone,Dolci,"[['Biscotti Digestive', '200g'], ['Burro', '100g'], ['Ricotta vaccina', '500g'], ['Formaggio fresco spalmabile', '250g'], ['Succo di limone', '100g'], ['Zucchero a velo', '150g'], ['Scorza di limone', '2'], ['Panna fresca liquida', '100g'], ['Gelatina in fogli', '10g'], ['Succo di limone', '60g'], ['Amido di mais (maizena)', '15g'], ['Acqua', '100g'], ['Zucchero', '80g'], ['Curcuma in polvere', '¼'], ['Limoni', '1'], ['Menta', 'q.b.'], ['Zucchero', '100g'], ['Acqua', '80']]"
Tiramisù senza uova,Dolci,"[['Panna fresca liquida', '100g'], ['Mascarpone', '350g'], ['Caffè', '50g'], ['Savoiardi', '8'], ['Cacao amaro in polvere', 'q.b.'], ['Zucchero a velo', '30g']]"
Pasta Sfoglia,Dolci,"[['Farina 00', '175g'], ['Acqua', '100g'], ['Sale fino', '5g'], ['Burro', '250g'], ['Farina 00', '150g']]"
Patate duchessa,Contorni,"[['Patate', '500g'], ['Noce moscata', 'q.b.'], ['Pepe nero', 'q.b.'], ['Burro', '50g'], ['Tuorli', '2'], ['Parmigiano Reggiano DOP', '50g']]"
Gamberoni al forno,Secondi piatti,"[['Gamberoni', '12'], ['Succo di limone', '40g'], [""Olio extravergine d'oliva"", '60g'], ['Sale fino', 'q.b.'], ['Pepe nero', 'q.b.'], ['Prezzemolo', 'q.b.']]"
Torta foresta nera,Dolci,"[['Uova', '6'], ['Farina 00', '100g'], ['Cioccolato fondente', '140g'], ['Burro', '75g'], ['Zucchero', '180g'], ['Amido di mais (maizena)', '50g'], ['Lievito in polvere per dolci', '16g'], ['Ciliegie', '500g'], ['Zucchero', '80g'], ['Kirsch', '70g'], ['Panna fresca liquida', '1l'], ['Zucchero a velo', '80g'], ['Ciliegie', '17'], ['Cioccolato fondente', '150g'], ['Cioccolato fondente', '100g']]"
Torta allo yogurt,Dolci,"[['Yogurt greco', '320g'], ['Uova', '220g'], ['Burro', '150g'], ['Zucchero', '200g'], ['Farina 00', '250g'], ['Amido di mais (maizena)', '80g'], ['Scorza di limone', '1'], ['Sale fino', '1pizzico'], ['Lievito in polvere per dolci', '16g']]"
Torta Pan di Stelle,Dolci,"[['Biscotti', '450g'], ['Panna fresca liquida', '600g'], ['Cioccolato fondente', '100g'], ['Zucchero a velo', '80g'], ['Latte intero', '70g'], ['Latte intero', 'q.b.'], ['Zucchero a velo', 'q.b.'], ['Cacao amaro in polvere', 'q.b.']]"
Frollini al Parmigiano,Antipasti,"[['Parmigiano Reggiano DOP', '100g'], ['Farina 00', '125g'], ['Burro salato', '80g'], ['Sale fino', '1pizzico'], ['Pepe nero', '1pizzico'], ['Albumi', '1'], ['Pinoli', 'q.b.'], ['Cumino', 'q.b.'], ['Sale affumicato', 'q.b.'], ['Pepe di Sichuan', 'q.b.']]"
Pasta biscotto,Dolci,"[['Uova', '5'], ['Miele', '10g'], ['Farina', '100g'], ['Zucchero', '140g'], ['Vaniglia', '1']]"
Stelle (biscotti) di Natale,Dolci,"[['Farina 00', '350g'], ['Burro', '250g'], ['Zucchero', '150g'], ['Tuorli', '4'], ['Lievito in polvere per dolci', '8g'], ['Vaniglia', '½'], [""Scorza d'arancia"", '1'], ['Albumi', '1'], ['Zucchero di canna', 'q.b.']]"
Bretzel,Lievitati,"[['Farina 00', '340g'], ['Acqua', '185g'], ['Sale fino', '5g'], ['Lievito di birra secco', '2g'], ['Burro', '15g'], ['Fiocchi di sale', 'q.b.'], ['Bicarbonato', '35g'], ['Acqua', '2l']]"
Nutellotti,Dolci,"[['Nutella', '180g'], ['Farina 00', '135g'], ['Uova', '1'], ['Nutella', '125g'], ['Granella di nocciole', '30g']]"
"Fagottini di porri, salmone e robiola",Primi piatti,"[['Uova', '2'], ['Latte', '300g'], ['Farina 00', '150g'], ['Burro', '40g'], ['Sale fino', '1pizzico'], ['Porri', '500g'], ['Robiola', '100g'], ['Salmone', '100g'], ['Grana Padano DOP', '50g'], ['Noce moscata', 'q.b.'], ['Pepe', 'q.b.'], ['Latte', '100g'], ['Sale fino', 'q.b.'], ['Timo', '3'], ['Aglio', '1']]"
Cheesecake alla Nutella,Dolci,"[['Biscotti Digestive', '180g'], ['Burro', '80g'], ['Nutella', '500g'], ['Formaggio fresco spalmabile', '500g'], ['Granella di nocciole', '30g']]"
Meringa alla francese (impasto base),Dolci,"[['Albumi', '100g'], ['Zucchero a velo', '220g'], ['Succo di limone', 'q.b.']]"
Polpette di pesce,Secondi piatti,"[['Merluzzo', '700g'], ['Pane', '100g'], ['Prezzemolo', '1'], ['Timo', 'q.b.'], ['Uova', '2'], ['Aglio', '1'], ['Sale fino', 'q.b.'], ['Pepe nero', 'q.b.'], ['Parmigiano Reggiano DOP', '80g'], ['Farina 00', 'q.b.'], ['Olio di semi di arachide', 'q.b.']]"
Frittelle di mele,Dolci,"[['Mele verdi', '580g'], ['Succo di limone', '1'], ['Farina 00', '150g'], ['Latte intero', '200g'], ['Uova', '2'], ['Lievito in polvere per dolci', '8g'], ['Sale fino', '1pizzico'], ['Olio di semi', 'q.b.'], ['Zucchero', '30g'], ['Cannella in polvere', 'q.b.']]"
"""