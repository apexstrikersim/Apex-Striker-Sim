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
            ClubTemplate("Ashcombe United", "ELITE"),
            ClubTemplate("Blackfriars City", "ELITE"),
            ClubTemplate("Redcliffe Rovers", "BIG"),
            ClubTemplate("Marchwood Athletic", "BIG"),
            ClubTemplate("Falcondale Town", "BIG"),
            ClubTemplate("Whitmere Wanderers", "MID"),
            ClubTemplate("Elmsgate FC", "MID"),
            ClubTemplate("Thornbury United", "MID"),
            ClubTemplate("Kingswell Athletic", "MID"),
            ClubTemplate("Oakhaven Rovers", "MID"),
            ClubTemplate("Camborne City", "MID"),
            ClubTemplate("Brimshaw Town", "SMALL"),
            ClubTemplate("Netherfield United", "SMALL"),
            ClubTemplate("Stonebridge Athletic", "SMALL"),
            ClubTemplate("Harrowvale FC", "SMALL"),
            ClubTemplate("Ravensgate Rovers", "SMALL")
        ),
        "Spain" to listOf(
            ClubTemplate("Real Montenara", "ELITE"),
            ClubTemplate("Atlético Serrano", "ELITE"),
            ClubTemplate("Deportivo Vallesur", "BIG"),
            ClubTemplate("Unión Cardosa", "BIG"),
            ClubTemplate("Real Belmonte", "BIG"),
            ClubTemplate("CD Alcázar Norte", "MID"),
            ClubTemplate("Real Peñablanca", "MID"),
            ClubTemplate("Deportivo Marejada", "MID"),
            ClubTemplate("CD Rioseco", "MID"),
            ClubTemplate("Unión Fuenteluz", "MID"),
            ClubTemplate("Real Carrascal", "MID"),
            ClubTemplate("CD Sotoverde", "SMALL"),
            ClubTemplate("Deportivo Ibarreta", "SMALL"),
            ClubTemplate("Unión Montoro", "SMALL"),
            ClubTemplate("Real Vega Alta", "SMALL"),
            ClubTemplate("CD Almendrar", "SMALL")
        ),
        "France" to listOf(
            ClubTemplate("Racing Vallonne", "ELITE"),
            ClubTemplate("AS Beaumont", "ELITE"),
            ClubTemplate("Olympique Cardère", "BIG"),
            ClubTemplate("FC Montrieux", "BIG"),
            ClubTemplate("Stade Belcourt", "BIG"),
            ClubTemplate("AS Fontenoy", "MID"),
            ClubTemplate("FC Rochelande", "MID"),
            ClubTemplate("Olympique Vireux", "MID"),
            ClubTemplate("US Clairval", "MID"),
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
            ClubTemplate("Borussia Kaltenau", "BIG"),
            ClubTemplate("TSV Falkenheim", "BIG"),
            ClubTemplate("1. FC Hartmoor", "BIG"),
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
            ClubTemplate("1. FC Nesselrode", "SMALL")
        ),
        "Italy" to listOf(
            ClubTemplate("AC Montefiore", "ELITE"),
            ClubTemplate("Reale Cassano", "ELITE"),
            ClubTemplate("Unione Belverde", "BIG"),
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
