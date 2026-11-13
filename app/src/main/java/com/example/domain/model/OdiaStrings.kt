package com.example.domain.model

object OdishaCurriculum {
    val districts = listOf(
        "All Odisha (ସମଗ୍ର ଓଡ଼ିଶା)",
        "Angul (ଅନୁଗୁଳ)",
        "Balangir (ବଲାଙ୍ଗୀର)",
        "Balasore (ବାଲେଶ୍ୱର)",
        "Bargarh (ବରଗଡ଼)",
        "Bhadrak (ଭଦ୍ରକ)",
        "Boudh (ବୌଦ୍ଧ)",
        "Cuttack (କଟକ)",
        "Deogarh (ଦେବଗଡ଼)",
        "Dhenkanal (ଢେଙ୍କାନାଳ)",
        "Gajapati (ଗଜପତି)",
        "Ganjam (ଗଞ୍ଜାମ)",
        "Jagatsinghpur (ଜଗତସିଂହପୁର)",
        "Jajpur (ଯାଜପୁର)",
        "Jharsuguda (ଝାରସୁଗୁଡ଼ା)",
        "Kalahandi (କଳାହାଣ୍ଡି)",
        "Kandhamal (କନ୍ଧମାଳ)",
        "Kendrapara (କେନ୍ଦ୍ରାପଡ଼ା)",
        "Kendujhar (କେନ୍ଦୁଝର)",
        "Khordha (ଖୋର୍ଦ୍ଧା)",
        "Koraput (କୋରାପୁଟ)",
        "Malkangiri (ମାଲକାନଗିରି)",
        "Mayurbhanj (ମୟୂରଭଞ୍ଜ)",
        "Nabarangpur (ନବରଙ୍ଗପୁର)",
        "Nayagarh (ନୟାଗଡ଼)",
        "Nuapada (ନୂଆପଡ଼ା)",
        "Puri (ପୁରୀ)",
        "Rayagada (ରାୟଗଡ଼ା)",
        "Sambalpur (ସମ୍ବଲପୁର)",
        "Subarnapur (ସୁବର୍ଣ୍ଣପୁର)",
        "Sundargarh (ସୁନ୍ଦରଗଡ଼)"
    )

    val subjectsByStage: Map<NepStage, List<String>> = mapOf(
        NepStage.FOUNDATIONAL to listOf(
            "ମାତୃଭାଷା ଓଡ଼ିଆ (Odia Literacy)",
            "ମୌଳିକ ଗଣିତ (Foundational Numeracy)",
            "ପରିବେଶ ଓ ପ୍ରକୃତି (Nature & My World)",
            "କଳା, ଖେଳ ଓ କାହାଣୀ (Art, Play & Storytelling)",
            "English Readiness"
        ),
        NepStage.PREPARATORY to listOf(
            "ଓଡ଼ିଆ ସାହିତ୍ୟ (Odia Language)",
            "ଗଣିତ (Mathematics)",
            "ଆମ ପରିବେଶ (EVS / Our Environment)",
            "English",
            "କଳା ଓ ଶାରୀରିକ ଶିକ୍ଷା (Art & Physical Ed)"
        ),
        NepStage.MIDDLE to listOf(
            "ଓଡ଼ିଆ ସାହିତ୍ୟ (Odia)",
            "ଗଣିତ (Mathematics)",
            "ବିଜ୍ଞାନ (Science)",
            "ସାମାଜିକ ବିଜ୍ଞାନ (Social Science)",
            "English",
            "ସଂସ୍କୃତ / ହିନ୍ଦୀ (Sanskrit / Hindi)",
            "ବୃତ୍ତିମୂଳକ ଓ କମ୍ପ୍ୟୁଟର (Vocational & Coding)"
        ),
        NepStage.SECONDARY to listOf(
            "ଓଡ଼ିଆ ସାହିତ୍ୟ (Odia)",
            "ଗଣିତ (Mathematics)",
            "ଭୌତିକ ବିଜ୍ଞାନ (Physical Science)",
            "ଜୀବ ବିଜ୍ଞାନ (Life Science)",
            "ଇତିହାସ ଓ ରାଜନୀତି ବିଜ୍ଞାନ (History & Civics)",
            "ଭୂଗୋଳ ଓ ଅର୍ଥନୀତି (Geography & Economics)",
            "English"
        )
    )

    val mediums = listOf(
        "ଓଡ଼ିଆ ମାଧ୍ୟମ (Odia Medium)",
        "ଇଂରାଜୀ ମାଧ୍ୟମ (English Medium)",
        "ଦ୍ୱିଭାଷୀ (Bilingual Odia + English)"
    )

    val boards = listOf(
        "SCERT Odisha / OSEPA",
        "BSE Odisha (ମାଧ୍ୟମିକ ଶିକ୍ଷା ପରିଷଦ)",
        "CHSE Odisha",
        "OAVS / CBSE"
    )
}

fun L(lang: AppLanguage, odia: String, english: String): String =
    if (lang == AppLanguage.ODIA) odia else english
