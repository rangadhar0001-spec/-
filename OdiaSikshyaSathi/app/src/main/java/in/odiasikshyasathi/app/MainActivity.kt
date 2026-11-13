package in.odiasikshyasathi.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Navy = Color(0xFF123C91)
private val Blue = Color(0xFF1976D2)
private val Pale = Color(0xFFF3F7FF)
private val Green = Color(0xFF168447)
private val Orange = Color(0xFFF28C00)
private val Pink = Color(0xFFD81B60)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SikshyaApp() }
    }
}

@Composable
fun SikshyaApp() {
    var tab by remember { mutableStateOf("ହୋମ୍") }
    MaterialTheme(
        colorScheme = lightColorScheme(primary = Navy, secondary = Green, background = Pale)
    ) {
        Scaffold(
            containerColor = Pale,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ଓଡ଼ିଆ ଶିକ୍ଷା ସାଥୀ", fontWeight = FontWeight.Bold)
                            Text("ଶିକ୍ଷକ • ଛାତ୍ର • ଶିକ୍ଷା ସମୁଦାୟ", fontSize = 11.sp)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = Navy
                    )
                )
            },
            bottomBar = {
                NavigationBar(containerColor = Color.White) {
                    val items = listOf(
                        Triple("ହୋମ୍", Icons.Default.Home, "Home"),
                        Triple("ଲେସନ୍", Icons.Default.MenuBook, "Lesson"),
                        Triple("ସମୁଦାୟ", Icons.Default.Groups, "Community"),
                        Triple("ପ୍ରୋଫାଇଲ୍", Icons.Default.Person, "Profile")
                    )
                    items.forEach { (label, icon, _) ->
                        NavigationBarItem(
                            selected = tab == label,
                            onClick = { tab = label },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label, fontSize = 10.sp) }
                        )
                    }
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                when (tab) {
                    "ହୋମ୍" -> HomeScreen(
                        onCreateLesson = { tab = "ଲେସନ୍" },
                        onCommunity = { tab = "ସମୁଦାୟ" }
                    )
                    "ଲେସନ୍" -> LessonBuilder()
                    "ସମୁଦାୟ" -> CommunityScreen()
                    else -> ProfileScreen()
                }
            }
        }
    }
}

@Composable
fun HomeScreen(onCreateLesson: () -> Unit, onCommunity: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Navy),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("ଶିଖିବା, ଶିଖାଇବା, ଏକାଠି ବଢ଼ିବା", color = Color.White,
                        fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("ଓଡ଼ିଶାର ଶିକ୍ଷକ ଓ ଶିକ୍ଷାର୍ଥୀଙ୍କ ପାଇଁ ଏକ ଶିକ୍ଷା ସମୁଦାୟ।",
                        color = Color.White)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onCreateLesson, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC928))) {
                        Icon(Icons.Default.EditNote, contentDescription = null, tint = Navy)
                        Spacer(Modifier.width(8.dp))
                        Text("ପଞ୍ଚପଦୀ ଲେସନ୍ ପ୍ଲାନ୍ ବନାନ୍ତୁ", color = Navy)
                    }
                }
            }
        }
        item { Text("NEP 2020 • 5+3+3+4 ଶିକ୍ଷା ଢାଞ୍ଚା", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Navy) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StageCard("୧. ମୌଳିକ ପର୍ଯ୍ୟାୟ", "ବୟସ ୩–୮ • ପ୍ରାକ୍-ବିଦ୍ୟାଳୟ ଓ ଶ୍ରେଣୀ ୧–୨", Pink)
                StageCard("୨. ପ୍ରସ୍ତୁତିମୂଳକ ପର୍ଯ୍ୟାୟ", "ଶ୍ରେଣୀ ୩–୫", Green)
                StageCard("୩. ମଧ୍ୟମ ପର୍ଯ୍ୟାୟ", "ଶ୍ରେଣୀ ୬–୮", Orange)
                StageCard("୪. ମାଧ୍ୟମିକ ପର୍ଯ୍ୟାୟ", "ଶ୍ରେଣୀ ୯–୧୨", Blue)
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp)) {
                    Text("ଶିକ୍ଷକ ସମୁଦାୟ", fontWeight = FontWeight.Bold, color = Navy, fontSize = 18.sp)
                    Text("ଲେସନ୍ ପ୍ଲାନ୍, କାର୍ଯ୍ୟକଳାପ ଓ ଶିକ୍ଷାଦାନ ଧାରଣା ଅନ୍ୟମାନଙ୍କ ସହ ସେୟାର୍ କରନ୍ତୁ।")
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onCommunity) { Text("ସମୁଦାୟ ଦେଖନ୍ତୁ") }
                }
            }
        }
    }
}

@Composable
fun StageCard(title: String, subtitle: String, color: Color) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp, 48.dp).background(color, RoundedCornerShape(8.dp)))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, color = color)
                Text(subtitle, fontSize = 13.sp, color = Color.DarkGray)
            }
        }
    }
}

@Composable
fun LessonBuilder() {
    var grade by remember { mutableStateOf("୩") }
    var subject by remember { mutableStateOf("ପରିବେଶ ଅଧ୍ୟୟନ") }
    var topic by remember { mutableStateOf("ଉଦ୍ଭିଦର ଉପକାରିତା") }
    var duration by remember { mutableStateOf("୪୦ ମିନିଟ୍") }
    var generated by remember { mutableStateOf(false) }
    val steps = listOf(
        "ଆଦିତି" to "ପୂର୍ବଜ୍ଞାନ ଜାଣିବା ପାଇଁ ଗଛ ବିଷୟରେ ଛୋଟ ଆଲୋଚନା କରନ୍ତୁ।",
        "ବୋଧ" to "ଗଛର ଅଂଶ ଓ ଉପକାରିତା ଚିତ୍ର ଓ ଉଦାହରଣ ସହ ବୁଝାନ୍ତୁ।",
        "ଅଭ୍ୟାସ" to "ଗଛର ଚିତ୍ର ଆଙ୍କି ଅଂଶଗୁଡ଼ିକ ଚିହ୍ନଟ କରାନ୍ତୁ।",
        "ପ୍ରୟୋଗ" to "ବିଦ୍ୟାଳୟ କିମ୍ବା ଘର ପାଖରେ ଥିବା ଗଛ ନିରୀକ୍ଷଣ କରାନ୍ତୁ।",
        "ପ୍ରସାର" to "ଗଛ ସୁରକ୍ଷା ବିଷୟରେ ଛୋଟ ପୋଷ୍ଟର୍ କିମ୍ବା ପ୍ରକଳ୍ପ କରାନ୍ତୁ।"
    )
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("ପଞ୍ଚପଦୀ ଲେସନ୍ ପ୍ଲାନ୍", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Navy)
        Text("ବିଷୟବସ୍ତୁ ଭରନ୍ତୁ; ନମୁନା ପ୍ଲାନ୍ ତିଆରି ହେବ।", color = Color.DarkGray)
        OutlinedTextField(value = grade, onValueChange = { grade = it }, label = { Text("ଶ୍ରେଣୀ") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("ବିଷୟ") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = topic, onValueChange = { topic = it }, label = { Text("ପାଠର ବିଷୟ") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = duration, onValueChange = { duration = it }, label = { Text("ସମୟ") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { generated = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("ନମୁନା ଲେସନ୍ ପ୍ଲାନ୍ ତିଆରି କରନ୍ତୁ")
        }
        if (generated) {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(topic, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Navy)
                    Text("ଶ୍ରେଣୀ: $grade  •  ବିଷୟ: $subject  •  ସମୟ: $duration")
                    Text("ଶିକ୍ଷଣ ଉଦ୍ଦେଶ୍ୟ: ଶିକ୍ଷାର୍ଥୀମାନେ ଗଛର ଉପକାରିତା ବୁଝି ଉଦାହରଣ ଦେଇପାରିବେ।")
                    steps.forEachIndexed { index, step ->
                        Divider()
                        Text("${index + 1}. ${step.first}", fontWeight = FontWeight.Bold, color = Green)
                        Text(step.second)
                    }
                    Text("ଶିକ୍ଷକଙ୍କ ଟିପ୍ପଣୀ: ଏହା ଏକ ନମୁନା ଡ୍ରାଫ୍ଟ। ନିଜ ପାଠ୍ୟକ୍ରମ ଅନୁଯାୟୀ ସଂଶୋଧନ କରନ୍ତୁ।",
                        fontSize = 12.sp, color = Color.DarkGray)
                }
            }
        }
    }
}

@Composable
fun CommunityScreen() {
    val posts = listOf(
        Triple("ସୁଜାତା ମହାନ୍ତି", "ଶ୍ରେଣୀ ୩ • ପରିବେଶ ଅଧ୍ୟୟନ", "ଉଦ୍ଭିଦର ଅଂଶ ଶିଖାଇବା ପାଇଁ ଚିତ୍ର ଓ ପ୍ରକୃତ ପତ୍ର ବ୍ୟବହାର କରନ୍ତୁ।"),
        Triple("ରମେଶ ନାୟକ", "ଶ୍ରେଣୀ ୫ • ଗଣିତ", "ଦୈନିକ ଜୀବନର ଉଦାହରଣରେ ଭାଗ ଓ ମାପ ବୁଝାଇବା ଉପଯୋଗୀ।"),
        Triple("ଶିକ୍ଷା ସାଥୀ ଟିମ୍", "ସମ୍ପ୍ରଦାୟ ଅପଡେଟ୍", "ନିଜ ଲେସନ୍ ପ୍ଲାନ୍ ସେୟାର୍ କରିବା ପୂର୍ବରୁ ଛାତ୍ରଛାତ୍ରୀଙ୍କ ବ୍ୟକ୍ତିଗତ ତଥ୍ୟ ହଟାନ୍ତୁ।")
    )
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("ଶିକ୍ଷା ସମୁଦାୟ", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Navy) }
        item {
            OutlinedButton(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("ନୂଆ ପୋଷ୍ଟ (ଡେମୋ)")
            }
        }
        items(posts) { post ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp)) {
                    Text(post.first, fontWeight = FontWeight.Bold, color = Navy)
                    Text(post.second, fontSize = 12.sp, color = Green)
                    Spacer(Modifier.height(8.dp))
                    Text(post.third)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FavoriteBorder, contentDescription = null, tint = Pink)
                        Spacer(Modifier.width(5.dp))
                        Text("ପସନ୍ଦ")
                        Spacer(Modifier.width(18.dp))
                        Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = Blue)
                        Spacer(Modifier.width(5.dp))
                        Text("ମତାମତ")
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileScreen() {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("ମୋର ପ୍ରୋଫାଇଲ୍", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Navy)
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(18.dp)) {
                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Navy, modifier = Modifier.size(56.dp))
                Text("ଡେମୋ ବ୍ୟବହାରକାରୀ", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("ଭୂମିକା: ଶିକ୍ଷକ / ଶିକ୍ଷାର୍ଥୀ")
                Text("ଏହି ପ୍ରୋଜେକ୍ଟରେ ଲଗଇନ୍ ଓ ପ୍ରୋଫାଇଲ୍ ଡାଟା ଏଯାବତ୍ ସଂଯୋଗ ହୋଇନାହିଁ।", fontSize = 13.sp)
            }
        }
        Text("ପରବର୍ତ୍ତୀ ବିକାଶ ପର୍ଯ୍ୟାୟ", fontWeight = FontWeight.Bold, color = Navy)
        listOf("Firebase ଲଗଇନ୍", "ସତ୍ୟ ଡାଟାବେସ୍ ଓ ପୋଷ୍ଟ", "AI ଲେସନ୍ ପ୍ଲାନ୍ API", "PDF ରପ୍ତାନି", "ରିପୋର୍ଟ ଓ ସୁରକ୍ଷା ବ୍ୟବସ୍ଥା").forEach {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = Green)
                Spacer(Modifier.width(8.dp))
                Text(it)
            }
        }
    }
}
