package com.example.data

import kotlin.random.Random

data class FaceDescriptor(
    val skinTone: Int,       // index into SKIN_TONES
    val hairStyle: Int,      // index into HAIR_STYLES
    val hairColor: Int,      // index into HAIR_COLORS
    val eyeStyle: Int,       // index into EYE_STYLES
    val eyebrowStyle: Int,   // index into EYEBROW_STYLES
    val jawVariant: Int,     // index into JAW_VARIANTS (subtle face-shape variance)
    val hasBeardTrait: Boolean,  // whether this player is capable of growing a beard at all
    val beardStyle: Int,     // index into BEARD_STYLES (only rendered if hasBeardTrait)
    val beardRevealAge: Int  // age at which the beard starts appearing, if hasBeardTrait
) {
    fun serialize(): String =
        listOf(skinTone, hairStyle, hairColor, eyeStyle, eyebrowStyle, jawVariant,
            if (hasBeardTrait) 1 else 0, beardStyle, beardRevealAge).joinToString("-")

    companion object {
        // Counts must match the option lists PlayerFaceIcon.kt will define.
        const val SKIN_TONE_COUNT = 6
        const val HAIR_STYLE_COUNT = 8
        const val HAIR_COLOR_COUNT = 5
        const val EYE_STYLE_COUNT = 3
        const val EYEBROW_STYLE_COUNT = 3
        const val JAW_VARIANT_COUNT = 3
        const val BEARD_STYLE_COUNT = 4

        fun deserialize(raw: String): FaceDescriptor {
            val p = raw.split("-").mapNotNull { it.toIntOrNull() }
            if (p.size < 9) return random("ENG") // safe fallback for blank/corrupt/pre-migration rows
            return FaceDescriptor(p[0], p[1], p[2], p[3], p[4], p[5], p[6] == 1, p[7], p[8])
        }

        /** Nationality-weighted skin tone pool. Extend this map as new nations are added —
         *  unmapped nations fall through to a neutral default distribution. */
        private fun skinToneWeightsFor(nationality: String): List<Pair<Int, Int>> = when (nationality) {
            // (skinToneIndex, weight) — indices map to SKIN_TONES in PlayerFaceIcon.kt
            "ENG", "GER" -> listOf(0 to 40, 1 to 35, 2 to 15, 3 to 6, 4 to 3, 5 to 1)
            "ESP", "ITA" -> listOf(0 to 15, 1 to 35, 2 to 35, 3 to 10, 4 to 4, 5 to 1)
            "FRA" -> listOf(0 to 25, 1 to 30, 2 to 25, 3 to 12, 4 to 6, 5 to 2)
            else -> listOf(0 to 20, 1 to 30, 2 to 25, 3 to 15, 4 to 7, 5 to 3)
        }

        private fun weightedPick(weights: List<Pair<Int, Int>>, rng: Random): Int {
            val total = weights.sumOf { it.second }
            var roll = rng.nextInt(total)
            for ((index, weight) in weights) {
                if (roll < weight) return index
                roll -= weight
            }
            return weights.last().first
        }

        fun random(nationality: String, rng: Random = Random.Default): FaceDescriptor {
            val hasBeard = rng.nextInt(100) < 45
            return FaceDescriptor(
                skinTone = weightedPick(skinToneWeightsFor(nationality), rng),
                hairStyle = rng.nextInt(HAIR_STYLE_COUNT),
                hairColor = rng.nextInt(HAIR_COLOR_COUNT),
                eyeStyle = rng.nextInt(EYE_STYLE_COUNT),
                eyebrowStyle = rng.nextInt(EYEBROW_STYLE_COUNT),
                jawVariant = rng.nextInt(JAW_VARIANT_COUNT),
                hasBeardTrait = hasBeard,
                beardStyle = rng.nextInt(BEARD_STYLE_COUNT),
                beardRevealAge = rng.nextInt(20, 27)
            )
        }

        /** Heir's face resembles the father's: most facial features carry over,
         *  hair color has a very small chance (5%) to differ, while hair style is
         *  extremely likely (92%) to differ to give the heir their own unique modern look. */
        fun inheritedFrom(father: FaceDescriptor, nationality: String, rng: Random = Random.Default): FaceDescriptor {
            fun <T> maybeReroll(current: T, chancePct: Int, reroll: () -> T): T =
                if (rng.nextInt(100) < chancePct) reroll() else current

            val hasBeard = maybeReroll(father.hasBeardTrait, 15) { rng.nextInt(100) < 45 }
            
            // Extremely likely (92%) that the son picks a different hairstyle than his father
            val heirHairStyle = if (rng.nextInt(100) < 92) {
                val differentStyles = (0 until HAIR_STYLE_COUNT).filter { it != father.hairStyle }
                if (differentStyles.isNotEmpty()) differentStyles.random(rng) else rng.nextInt(HAIR_STYLE_COUNT)
            } else {
                father.hairStyle
            }

            // Very little chance (5%) of having a different hair color than the father
            val heirHairColor = maybeReroll(father.hairColor, 5) { rng.nextInt(HAIR_COLOR_COUNT) }

            return FaceDescriptor(
                skinTone = maybeReroll(father.skinTone, 10) { weightedPick(skinToneWeightsFor(nationality), rng) },
                hairStyle = heirHairStyle,
                hairColor = heirHairColor,
                eyeStyle = maybeReroll(father.eyeStyle, 20) { rng.nextInt(EYE_STYLE_COUNT) },
                eyebrowStyle = maybeReroll(father.eyebrowStyle, 20) { rng.nextInt(EYEBROW_STYLE_COUNT) },
                jawVariant = maybeReroll(father.jawVariant, 20) { rng.nextInt(JAW_VARIANT_COUNT) },
                hasBeardTrait = hasBeard,
                beardStyle = if (hasBeard) maybeReroll(father.beardStyle, 25) { rng.nextInt(BEARD_STYLE_COUNT) } else father.beardStyle,
                beardRevealAge = maybeReroll(father.beardRevealAge, 30) { rng.nextInt(20, 27) }
            )
        }
    }
}
