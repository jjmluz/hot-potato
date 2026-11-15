package com.jj.hotpotato

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

// Avatares em pixel art, 16x16. Cada letra é uma cor da paleta; "." é transparente.
private val PALETTE = mapOf(
    'k' to 0xFF2B1D16, // contorno
    's' to 0xFFF2C09B, // pele
    'S' to 0xFFD99A74, // pele (sombra)
    'l' to 0xFFFBE0CC, // pele clara
    'L' to 0xFFECBFA0, // pele clara (sombra)
    'd' to 0xFFA86A43, // pele morena
    'D' to 0xFF874F2E, // pele morena (sombra)
    'h' to 0xFF6B3F22, // cabelo castanho
    'q' to 0xFFB07A4A, // cabelo castanho claro
    'x' to 0xFF1C1A20, // cabelo preto
    'X' to 0xFF3D3A46, // cabelo preto (brilho)
    'e' to 0xFFD4D4D4, // cabelo grisalho
    'y' to 0xFFE8B64C, // amarelo / loiro
    'r' to 0xFFE5484D, // vermelho
    'b' to 0xFF4D8FE0, // azul
    'B' to 0xFFCFE6F7, // lentes
    't' to 0xFF3FB6A8, // verde-água
    'g' to 0xFF5AA860, // verde
    'v' to 0xFF8E6CD1, // roxo
    'w' to 0xFFFFFFFF, // branco
    'p' to 0xFFF28FB0, // rosa
    'm' to 0xFF858D97, // cinzento (gato)
    'M' to 0xFF5B626C, // cinzento escuro (gato)
    'z' to 0xFFA3ABB5, // cinzento claro (gatinho)
    'Z' to 0xFF6F7782, // cinzento (gatinho)
    'n' to 0xFFA06A3C, // castanho (cão)
    'N' to 0xFF6E4524, // castanho escuro
    'c' to 0xFFF3E2C4, // creme
)

class Avatar(val id: String, rows: List<String>) {
    val sprite: ImageBitmap = spriteBitmap(rows)
}

private fun spriteBitmap(rows: List<String>): ImageBitmap {
    val bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
    rows.forEachIndexed { y, row ->
        row.take(16).forEachIndexed { x, ch ->
            PALETTE[ch]?.let { bitmap.setPixel(x, y, it.toInt()) }
        }
    }
    return bitmap.asImageBitmap()
}

val AVATARS = listOf(
    Avatar( // mulher de cabelo preto
        "mulher",
        listOf(
            "................",
            ".....kkkkkk.....",
            "....kxxxxxXk....",
            "...kxxxxxxXXk...",
            "..kxxxxxxxxxXk..",
            "..kxllllllllxk..",
            "..kxlkllllklxk..",
            "..kxpllllllpxk..",
            "..kxlllrrlllxk..",
            "..kxxkllllkxxk..",
            "..kxxxkLLkxxxk..",
            "..kxxkrrrrkxxk..",
            "..kkrrrrrrrrkk..",
            ".krrrrrrrrrrrrk.",
            ".krrrrrrrrrrrrk.",
            ".kkkkkkkkkkkkkk.",
        ),
    ),
    Avatar( // menina de tranças
        "menina",
        listOf(
            "................",
            "......kkkk......",
            "....kkqqqqkk....",
            ".pp.kqqqqqqk.pp.",
            "pppkqqqqqqqqkppp",
            ".pkqqssssssqqkp.",
            "..kqssssssssqk..",
            "..kqskssssksqk..",
            "..kqpsssssspqk..",
            ".kqkssskkssskqk.",
            ".kqk.kssssk.kqk.",
            ".kk...kSSk...kk.",
            "....kkggggkk....",
            "...kggggggggk...",
            "..kggggwwggggk..",
            "..kkkkkkkkkkkk..",
        ),
    ),
    Avatar( // homem com óculos
        "homem",
        listOf(
            "................",
            ".....kkkkkk.....",
            "....khhhhhhk....",
            "...khhhhhhhhk...",
            "...khsssssshk...",
            "...kkkksskkkk...",
            "...kBBkkkkBBk...",
            "...kkkksskkkk...",
            "...kssskksssk...",
            "....kssssssk....",
            ".....kSSSSk.....",
            "...kkbbbbbbkk...",
            "..kbbbbwwbbbbk..",
            ".kbbbbbbbbbbbbk.",
            ".kbbbbbbbbbbbbk.",
            ".kkkkkkkkkkkkkk.",
        ),
    ),
    Avatar( // gato cinzento
        "gato",
        listOf(
            "................",
            "..kk........kk..",
            "..kpk......kpk..",
            "..kmpk....kpmk..",
            "..kmmmkkkkmmmk..",
            "..kmmmmmmmmmmk..",
            "..kmmMmmmmMmmk..",
            "..kmgkmmmmgkmk..",
            "..kmmmcppcmmmmk.",
            ".kkmmcckkccmmkk.",
            "...kmccccccmk...",
            "....kkmmkkmm....",
            "...kmmmmmmmmk...",
            "..kmmMmmmmMmmk..",
            "..kccmmmmmmcck..",
            "..kkkkkkkkkkkk..",
        ),
    ),
    Avatar( // gatinho cinzento
        "gatinho",
        listOf(
            "................",
            "................",
            "...kk......kk...",
            "...kzk....kzk...",
            "...kpzkkkkzpk...",
            "..kzzzzzzzzzzk..",
            "..kzZzzzzzzZzk..",
            "..kzkkzzzzkkzk..",
            "..kzkwzzzzkwzk..",
            ".kzzzccppcczzzk.",
            "..kzccckkccczk..",
            "...kzcccccczk...",
            "....kkzzzzkk....",
            "....kzzzzzzk....",
            "...kzcczzcczk...",
            "...kkkkkkkkkk...",
        ),
    ),
    Avatar( // homem grisalho com barba
        "avo",
        listOf(
            "................",
            "................",
            ".....kkkkkk.....",
            "....kssssssk....",
            "...kessssssek...",
            "...keskssksek...",
            "...kssssssssk...",
            "...kseeeeeesk...",
            "...keeekkeeek...",
            "....keeeeeek....",
            ".....keeeek.....",
            "...kkggggggkk...",
            "..kggggggggggk..",
            ".kggggggggggggk.",
            ".kggggggggggggk.",
            ".kkkkkkkkkkkkkk.",
        ),
    ),
    Avatar( // mulher loira com carrapito
        "loira",
        listOf(
            ".......kk.......",
            "......kyyk......",
            ".....kkyykk.....",
            "....kyyyyyyk....",
            "...kyyyyyyyyk...",
            "...kyssssssyk...",
            "...kskssssksk...",
            "...kpssssssspk..",
            "...ksssrrsssk...",
            "....kssssssk....",
            ".....kkSSkk.....",
            "...kkvvvvvvkk...",
            "..kvvvvwwvvvvk..",
            ".kvvvvvvvvvvvvk.",
            ".kvvvvvvvvvvvvk.",
            ".kkkkkkkkkkkkkk.",
        ),
    ),
    Avatar(
        "menino",
        listOf(
            "................",
            "....kk.kk.kk....",
            "...khhkhhkhhk...",
            "...khhhhhhhhk...",
            "...khhhhhhhhk...",
            "...khhssssssk...",
            "..kSskssssksSk..",
            "...kpssssssspk..",
            "...kssskksssk...",
            "....kssssssk....",
            ".....kkSSkk.....",
            "....kttttttk....",
            "...kttwttwttk...",
            "...kttttttttk...",
            "...kttttttttk...",
            "...kkkkkkkkkk...",
        ),
    ),
    Avatar( // menino moreno de cabelo encaracolado
        "menino-caracois",
        listOf(
            "................",
            "....kkkkkkkk....",
            "...kxxXxxXxxk...",
            "..kxXxxxxxxXxk..",
            "..kxxxxxxxxxxk..",
            "..kxxddddddxxk..",
            "..kDdkddddkdDk..",
            "...kpddddddpk...",
            "...kdddkkdddk...",
            "....kddddddk....",
            ".....kkDDkk.....",
            "....kyyyyyyk....",
            "...kyyybbyyyk...",
            "...kyyyyyyyyk...",
            "...kyyyyyyyyk...",
            "...kkkkkkkkkk...",
        ),
    ),
    Avatar( // menina de cabelo preto curto e fita vermelha
        "menina-franja",
        listOf(
            "................",
            ".....kkkkkk.....",
            "....kxxxxxxk....",
            "...krrrrrrrrk...",
            "..kxxxxxxxxxxk..",
            "..kxxssssssxxk..",
            "..kxskssssksxk..",
            "..kxpsssssspxk..",
            "..kxssskksssxk..",
            "..kxxksssskxxk..",
            "...kkkkSSkkkk...",
            "....kbbbbbbk....",
            "...kbbwbbwbbk...",
            "..kbbbbbbbbbbk..",
            "..kbbbbbbbbbbk..",
            "..kkkkkkkkkkkk..",
        ),
    ),
    Avatar(
        "cao",
        listOf(
            "................",
            ".....kkkkkk.....",
            "...kknnnnnnkk...",
            "..kNNnnnnnnNNk..",
            ".kNNNnnnnnnNNNk.",
            ".kNNNnnnnnnNNNk.",
            ".kNNNnknnknNNNk.",
            ".kNNNnnnnnnNNNk.",
            ".kNNnccccccnNNk.",
            "..kNccckkcccNk..",
            "...kcccppccck...",
            "....kcccccck....",
            ".....kkrrkk.....",
            "....knnyynnk....",
            "...knnnnnnnnk...",
            "...kkkkkkkkkk...",
        ),
    ),
    Avatar(
        "panda",
        listOf(
            "................",
            "..kkk......kkk..",
            ".kkkkk....kkkkk.",
            ".kkkkwwwwwwkkkk.",
            "..kkwwwwwwwwkk..",
            "..kwwwwwwwwwwk..",
            "..kwkkkwwkkkwk..",
            "..kwkwkwwkwkwk..",
            "..kwkkkwwkkkwk..",
            "..kwwwwkkwwwwk..",
            "...kwwwwwwwwk...",
            "....kkwwwwkk....",
            "...kkkwwwwkkk...",
            "..kkkkwwwwkkkk..",
            "..kkkkwwwwkkkk..",
            "..kkkkkkkkkkkk..",
        ),
    ),
)
