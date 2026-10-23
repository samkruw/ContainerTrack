package ch.zerogen.containertrack

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class LocalUserProblem(message:String):RuntimeException(message)

enum class LocalRole(val label:String) { ADMIN("Admin"), EMPLOYEE("Mitarbeiter") }
data class LocalUser(val id:String,val name:String,val role:LocalRole,val active:Boolean=true)
fun mayAccessLocalJob(user:LocalUser?,owner:String?) = user!=null && user.active && (user.role==LocalRole.ADMIN || user.id==owner)

object LocalPin {
    private const val ITERATIONS=210_000
    fun hash(pin:String):String {
        val salt=ByteArray(16).also{SecureRandom().nextBytes(it)}
        return "$ITERATIONS:${Base64.getEncoder().encodeToString(salt)}:${Base64.getEncoder().encodeToString(derive(pin,salt,ITERATIONS))}"
    }
    fun verify(pin:String,encoded:String):Boolean = runCatching {
        val parts=encoded.split(':');require(parts.size==3)
        val rounds=parts[0].toInt();require(rounds in 100_000..1_000_000)
        val salt=Base64.getDecoder().decode(parts[1]);require(salt.size==16)
        val expected=Base64.getDecoder().decode(parts[2]);require(expected.size==32)
        MessageDigest.isEqual(expected,derive(pin,salt,rounds))
    }.getOrDefault(false)
    private fun derive(pin:String,salt:ByteArray,rounds:Int):ByteArray {
        val spec=PBEKeySpec(pin.toCharArray(),salt,rounds,256)
        return try {SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded} finally {spec.clearPassword()}
    }
}
