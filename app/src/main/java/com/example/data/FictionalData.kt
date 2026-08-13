package com.example.data

object FictionalData {

    val COUNTRIES = listOf("England", "Spain", "France", "Germany", "Italy")

    // Trophy weightings
    const val WEIGHT_CHAMPIONS_LEAGUE = 10
    const val WEIGHT_LEAGUE = 8
    const val WEIGHT_EUROPA_LEAGUE = 7
    const val WEIGHT_CONFERENCE_LEAGUE = 5
    const val WEIGHT_SUPER_CUP = 3

    // Clubs data structures
    data class ClubTemplate(
        val name: String,
        val reputation: String // ELITE, BIG, MID, SMALL
    )

    val CLUB_TEMPLATES = mapOf(
        "England" to listOf(
            ClubTemplate("Manchester Blue", "ELITE"),
            ClubTemplate("Manchester Red", "ELITE"),
            ClubTemplate("London Claret", "BIG"),
            ClubTemplate("Liverpool Rovers", "BIG"),
            ClubTemplate("Birmingham Athletic", "BIG"),
            ClubTemplate("Leeds Athletic", "MID"),
            ClubTemplate("Newcastle Rovers", "MID"),
            ClubTemplate("Bristol Wanderers", "MID"),
            ClubTemplate("Nottingham Athletic", "MID"),
            ClubTemplate("Southampton Rovers", "MID"),
            ClubTemplate("Sheffield Wanderers", "MID"),
            ClubTemplate("Brighton Athletic", "SMALL"),
            ClubTemplate("Wolverhampton Rovers", "SMALL"),
            ClubTemplate("Burnley Athletic", "SMALL"),
            ClubTemplate("Fulham Rovers", "SMALL"),
            ClubTemplate("Brentford Athletic", "SMALL")
        ),
        "Spain" to listOf(
            ClubTemplate("Real Blanco", "ELITE"),
            ClubTemplate("FC Blaugrana", "ELITE"),
            ClubTemplate("Atlético Rojiblanco", "BIG"),
            ClubTemplate("Real Amarillo", "BIG"),
            ClubTemplate("Deportivo Verdiblanco", "BIG"),
            ClubTemplate("Real Peñablanca", "MID"),
            ClubTemplate("Unión Fuenteluz", "MID"),
            ClubTemplate("CD Rioseco", "MID"),
            ClubTemplate("Deportivo Marejada", "MID"),
            ClubTemplate("Real Carrascal", "MID"),
            ClubTemplate("CD Alcázar Norte", "MID"),
            ClubTemplate("Unión Montoro", "SMALL"),
            ClubTemplate("CD Sotoverde", "SMALL"),
            ClubTemplate("Deportivo Ibarreta", "SMALL"),
            ClubTemplate("Real Vega Alta", "SMALL"),
            ClubTemplate("CD Almendrar", "SMALL")
        ),
        "France" to listOf(
            ClubTemplate("Racing Bleu", "ELITE"),
            ClubTemplate("Olympique Rouge", "ELITE"),
            ClubTemplate("AS Paris Nord", "BIG"),
            ClubTemplate("Stade Marseillan", "BIG"),
            ClubTemplate("FC Lyonnais", "BIG"),
            ClubTemplate("AS Fontenoy", "MID"),
            ClubTemplate("Olympique Vireux", "MID"),
            ClubTemplate("US Clairval", "MID"),
            ClubTemplate("FC Rochelande", "MID"),
            ClubTemplate("AS Bellefosse", "MID"),
            ClubTemplate("FC Ardennois Sud", "MID"),
            ClubTemplate("US Marécourt", "SMALL"),
            ClubTemplate("AS Grandchamp", "SMALL"),
            ClubTemplate("FC Vaucresson Nord", "SMALL"),
            ClubTemplate("Olympique Terreblanche", "SMALL"),
            ClubTemplate("US Solignac", "SMALL")
        ),
        "Germany" to listOf(
            ClubTemplate("FC Rabenstein", "ELITE"),
            ClubTemplate("SV Wolfsbrück", "ELITE"),
            ClubTemplate("Borussia Gelb", "BIG"),
            ClubTemplate("TSV Falkenheim", "BIG"),
            ClubTemplate("FC Hartmoor", "BIG"),
            ClubTemplate("SC Grünwald Nord", "MID"),
            ClubTemplate("VfL Dornbach", "MID"),
            ClubTemplate("FC Steinfurt", "MID"),
            ClubTemplate("SV Lindenau", "MID"),
            ClubTemplate("TSV Achterberg", "MID"),
            ClubTemplate("Eintracht Moorstadt", "MID"),
            ClubTemplate("SpVgg Reichenau", "SMALL"),
            ClubTemplate("FC Bergheide", "SMALL"),
            ClubTemplate("TuS Waldkreuz", "SMALL"),
            ClubTemplate("SV Talhoven", "SMALL"),
            ClubTemplate("FC Nesselrode", "SMALL")
        ),
        "Italy" to listOf(
            ClubTemplate("AC Montefiore", "ELITE"),
            ClubTemplate("Reale Nerazzurro", "ELITE"),
            ClubTemplate("Unione Rossonera", "BIG"),
            ClubTemplate("AS Torresecca", "BIG"),
            ClubTemplate("Calcio Marinella", "BIG"),
            ClubTemplate("US Castelrosso", "MID"),
            ClubTemplate("AC Ripalunga", "MID"),
            ClubTemplate("Calcio Serravalle Sud", "MID"),
            ClubTemplate("AS Montorio", "MID"),
            ClubTemplate("US Falconara Nord", "MID"),
            ClubTemplate("AC Vignaverde", "MID"),
            ClubTemplate("US Portovento", "SMALL"),
            ClubTemplate("AS Colleoro", "SMALL"),
            ClubTemplate("Calcio Bassanera", "SMALL"),
            ClubTemplate("AC Fiumelago", "SMALL"),
            ClubTemplate("US Tremonti", "SMALL")
        )
    )

    // Name Pools for NPCs (Expanded to 50 authentic and diverse names per country)
    val FIRST_NAMES = mapOf(
        "England" to listOf(
            "Elliot", "Spencer", "Rowan", "Preston", "Miles", "Bertie", "Sidney", "Wesley", "Clive", "Terrence",
            "Percy", "Nigel", "Desmond", "Barnaby", "Gilbert", "Reggie", "Cyril", "Norris", "Wilfred", "Herbert",
            "Alistair", "Rupert", "Vernon", "Malcolm", "Trevor", "Colin", "Gavin", "Derek", "Barry", "Neville"
        ),
        "Spain" to listOf(
            "Emilio", "Ramon", "Ignacio", "Salvador", "Cesar", "Tomas", "Vicente", "Joaquin", "Gregorio", "Anselmo",
            "Baldomero", "Cipriano", "Doroteo", "Eusebio", "Gaspar", "Heliodoro", "Isidro", "Justo", "Laureano", "Marcelino",
            "Nemesio", "Onofre", "Prudencio", "Quirino", "Rogelio", "Saturnino", "Teodoro", "Urbano", "Valeriano", "Abelardo"
        ),
        "France" to listOf(
            "Baptiste", "Fabrice", "Gontran", "Herve", "Isidore", "Jerome", "Marius", "Norbert", "Octave", "Philippe",
            "Quentin", "Regis", "Serge", "Thibault", "Ulysse", "Valentin", "Wilfrid", "Xavier", "Yves", "Zacharie",
            "Amedee", "Bertrand", "Casimir", "Didier", "Emile", "Firmin", "Gaston", "Honore", "Ignace", "Julien"
        ),
        "Germany" to listOf(
            "Reinhard", "Gunther", "Dieter", "Helmut", "Ottokar", "Bernhardt", "Conrad", "Diether", "Eberhard", "Falk",
            "Gerold", "Hartwig", "Ingo", "Jost", "Klaus", "Lorenz", "Manfred", "Norbert", "Oskar", "Peter",
            "Quirin", "Reto", "Siegfried", "Traugott", "Udo", "Volker", "Wolfram", "Xaver", "York", "Ansgar"
        ),
        "Italy" to listOf(
            "Ottavio", "Bastiano", "Cirillo", "Dario", "Emidio", "Fabrizio", "Gualtiero", "Ilario", "Learco", "Massimiliano",
            "Nunzio", "Osvaldo", "Pierfranco", "Quintino", "Romualdo", "Sansone", "Taddeo", "Ubaldo", "Valentino", "Wladimiro",
            "Zeno", "Amerigo", "Benedetto", "Cataldo", "Donato", "Eligio", "Fortunato", "Gioacchino", "Isidoro", "Ludovico"
        )
    )

    val LAST_NAMES = mapOf(
        "England" to listOf(
            "Ashworth", "Bramwell", "Colegate", "Dunmore", "Elstone", "Farebrother", "Gosling", "Hartswell", "Ingleby", "Jarrow",
            "Kirkland", "Larkspur", "Mossop", "Nettlefold", "Oswick", "Pembridge", "Quennell", "Ridgeway", "Southwold", "Underhill",
            "Vellacott", "Wraxall", "Yardsley", "Beckwith", "Cranleigh", "Dalgleish", "Enderby", "Fenwright", "Gatesby", "Hollowell"
        ),
        "Spain" to listOf(
            "Escalante", "Membrillo", "Roldan", "Cifuentes", "Bermudo", "Aldecoa", "Villaverde", "Cordovilla", "Espinar", "Frontera",
            "Guijarro", "Hontanar", "Iruela", "Jarales", "Ledesma", "Montalban", "Novales", "Ordonez", "Pradillo", "Quijano",
            "Robledillo", "Sotomonte", "Trujillano", "Ubeda", "Valderas", "Yuncares", "Zaldivar", "Aguadulce", "Belorado", "Carrizal"
        ),
        "France" to listOf(
            "Roquefort", "Beaulande", "Chastenet", "Delombre", "Esparron", "Fontanel", "Grandvaux", "Herisson", "Illiers", "Jonquiere",
            "Lantier", "Marchetout", "Noireau", "Orbigny", "Pelloux", "Quernec", "Ribeaux", "Sabline", "Taillefer", "Uzeau",
            "Valcourt", "Verlin", "Wattier", "Xambeau", "Ysambert", "Aubertin", "Brissac", "Cambon", "Dorval", "Escoffier"
        ),
        "Germany" to listOf(
            "Vogelsang", "Wintermann", "Falkenrath", "Bergisch", "Dornfeld", "Eichinger", "Feuerbach", "Grunewald", "Hallstadt", "Isenberg",
            "Jachmann", "Kaltenborn", "Lindenthal", "Moosbrugger", "Nesenberg", "Ostendorf", "Pfannkuch", "Quambusch", "Rehberger", "Sonnenschein",
            "Tiefenbach", "Uhlmann", "Vollbrecht", "Wackernagel", "Ziegenhorn", "Aschenbrenner", "Birkenfeld", "Dachsberg", "Ehrenfeld", "Falkenstein"
        ),
        "Italy" to listOf(
            "Bellandi", "Castagnaro", "Draghetti", "Falconieri", "Guerrazzi", "Malandrino", "Nicastro", "Orlandelli", "Pagnotta", "Quarantotto",
            "Ravagnani", "Salvarezza", "Tortoretti", "Urbinati", "Vallecchi", "Zanotelli", "Bruscagli", "Cortellazzi", "Delfino", "Fiordelisi",
            "Grimaudo", "Iacovelli", "Lanzarotti", "Montebello", "Nardozzi", "Pellicano", "Quercioli", "Rocchetti", "Savignano", "Tebaldini"
        )
    )

    fun generateRandomName(country: String): String {
        val firsts = FIRST_NAMES[country] ?: FIRST_NAMES["England"]!!
        val lasts = LAST_NAMES[country] ?: LAST_NAMES["England"]!!
        return "${firsts.random()} ${lasts.random()}"
    }

    fun getReputationPoints(reputation: String): Int {
        return when (reputation) {
            "ELITE" -> 90
            "BIG" -> 75
            "MID" -> 50
            "SMALL" -> 25
            else -> 25
        }
    }

    fun getRivalOvrRange(reputation: String): IntRange {
        return when (reputation) {
            "ELITE" -> 80..88
            "BIG" -> 73..81
            "MID" -> 64..72
            "SMALL" -> 54..63
            else -> 50..60
        }
    }
}
