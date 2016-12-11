/**
 * Created by arty on 12/10/16.
 */

package org.sample

val HERO_RES = "KarlSheet.png"
val SKINNER_RES = "WalterSheet.png"
val COP_RES = "CopSheet01.png"

val TYPEWRITER_RES = "typewriter.png"
val FILECABINET_RES = "filecabinet.png"
val KEYCARD_RES = "keycard.png"

val AnimRestForward = listOf(16)
val AnimRestLeft = listOf(17)
val AnimRestRight = listOf(18)

val AnimWalkLeft = (20..29).toList()
val AnimWalkRight = (1..10).toList()

val dims = mapOf(KEYCARD_RES.to(Pair<Double,Double>(0.5,0.5)), TYPEWRITER_RES.to(Pair<Double,Double>(2.0,2.0)))
val vicText = """An unassuming office building in any town in central Ohio just might be the site of the biggest
        conspiracy since watergate ... if any physical evidence survived this morning's devastating fire.  The office,
        officially used to store tax records from the decades before electronic bookkeeping proved categorically false,
        but the real purpose of the occasional kilowatt jolt and the constant movement of high voltage equipment through
        the office remains a mystery, explained only as a series of clerical errors in official documentation"""
val introText = """Saturday, August 5th, 1972.  Upon receiving reports of unexplained activity involving high voltage
        equipment in a central Ohio office building, you, intrepid reporter, vow to obtain the scoop of a lifetime.
        Workers complaining of sullen men in dark suits, haunting sounds and fearful odors allude to "one room" where
        'they' carry out their sinister agenda.  Using arrow keys, 'C' to call an elevator, 'S' to stun with your camera
        flash, and 'Space' to interact, infiltrate the conspiracy and find the "one room".  The truth is out there."""