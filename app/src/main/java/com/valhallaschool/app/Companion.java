package com.valhallaschool.app;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;

import static com.valhallaschool.app.Ui.*;

/**
 * Acompañante inmersivo de los ejercicios: un personaje de Binky GRANDE
 * (~210dp) con bocadillo, protagonista de la pantalla, no un icono en
 * una esquina. Cambia en cada ejercicio (baraja de los 48 avatares, sin
 * repetir hasta agotarla) y se mueve SIEMPRE: rebote suave en bucle,
 * saludo al aparecer, celebración al acertar y decepción al fallar.
 *
 * DOS TONOS (orden de Remy 2026-11-27, v1.3.2):
 * - GANAR / ACERTAR: sarcasmo y desprestigio, pero SIN INSULTOS: una línea
 *   de indiferencia sobre si aciertas («Ah, una bien. Tampoco te emociones»).
 * - FALLAR: el personaje SE ENOJA: expresión de disgusto extremo (emoji
 *   grande 🤢😤 + marco rojo) y un regaño que se mete contigo y con tu
 *   persona: tu papá, tu mamá, tu perro o gato, tu ex, tu inteligencia y
 *   tu físico. El sonido también cambia: acierto agradable, fallo de
 *   disgusto/enojo (Sfx.YAY / Sfx.ANGRY, centralizado aquí).
 * Nunca contra grupos protegidos (nada de raza, género, religión,
 * discapacidad ni orientación): la humillación es solo contra el jugador,
 * que lo pidió así. TODAS las frases usan el nombre actual (nickname o
 * «Sin nombre»).
 */
final class Companion {
    /** Ánimo ANTES de responder: sarcasmo indiferente, SIN insultos (orden de Remy 2026-11-27). */
    static final String[] CHEER = {
            "Vamos, {name}, a ver qué sale esta vez.",
            "Respira, {name}… que pensar no es tu fuerte, pero inténtalo.",
            "Esta es tuya, {name}. O eso dicen los que no te conocen.",
            "Dale, {name}. ¿Qué función será? Apuesto a que la fallas.",
            "Tú ya sabes esto, {name}… o eso crees tú.",
            "Vamos por otra, {name}. A ver cuánto aguantas sin llorar.",
            "¿Listo, {name}? A darle. Total, fallar es gratis.",
            "Tú puedes con esto, {name}. Bueno, eso dicen.",
            "Piensa como tipador, {name}… si es que puedes pensar.",
            "Otra más, {name}. A ver si esta no la arruinas como las demás.",
            "Ánimo, {name}. Yo te acompaño… alguien tiene que reírse.",
            "Mira bien la frase, {name}… aunque ni mirando la vas a sacar.",
            "Tú sí sabes, {name}. ¿O no? Ya ni sé.",
            "Vamos, {name}. Que nadie te pare. Bueno, tú mismo te paras solo.",
            "¿Otra ronda, {name}? Claro que sí. Total, fallar es gratis y tú eres experto.",
            "Tú puedes, {name}. Tú sí sabes… ¿o no? Ay, qué triste.",

    };

    /** Al ACERTAR: sarcasmo e indiferencia, SIN insultos (orden de Remy 2026-11-27). */
    static final String[] CELEBRATE = {
            "Ah, una bien, {name}. Tampoco te emociones.",
            "Vaya, hasta tú aciertas a veces, {name}.",
            "Mira, {name}, una correcta. No te acostumbres.",
            "Bien, {name}. Siguiente, que esto no se va a hacer solo.",
            "Una bien, {name}. Qué sorpresa, la verdad.",
            "Acertaste, {name}. No sé cómo, pero acertaste.",
            "Mira nada más, {name}, hasta acertaste. Qué milagro.",
            "Sí, {name}, esa era. No te emociones, que fue una.",
            "Genial, {name}. ¿Seguro que no fue suerte? Yo digo que sí.",
            "Increíble, {name}. Hasta tú la sacaste. Ni tú te la crees.",
            "Bravo, {name}. Una bien de cada cien, qué promedio.",
            "Imparable, {name}. Bueno, por esta vez.",

    };

    /** Al FALLAR: insultos por elemento (v1.3.6 fase 2): 25 por elemento,
     *  200 en total. Orden Ne, Ni, Se, Si, Te, Ti, Fe, Fi (igual que
     *  PracticeData.ELEMENTS). Cada frase pega en la función débil de ese
     *  elemento y juega con su señal sutil: Ne, ya nadie te enseña en qué
     *  se puede convertir nada; Ni, nadie te cuenta lo que viene; Se, ni
     *  te miran; Si, a nadie le importa si estás cómodo; Fe, nadie se ríe
     *  contigo; Te, nadie te pide hacer nada; Ti, nadie te explica las
     *  reglas; Fi, te hablan distinto / te contestan seco / ya no te
     *  hablan como antes (= te alejaste). Vulgar e informal, SOLO contra
     *  el jugador (que lo pidió), nunca contra grupos protegidos. Sin la
     *  palabra prohibida para Ne. El banco SCOLD viejo se REEMPLAZA por
     *  este (no se suma). */
    static final String[][] SCOLD_BY_ELEMENT = {
            // Ne: 25 frases
            {
                    "Ya nadie te enseña en qué se puede convertir nada, {name}, porque para qué, si ni lo vas a ver.",
                    "Nadie te muestra el potencial de nada, {name}, porque tú solo ves lo que ya está, pendejo.",
                    "Ya nadie te dice qué se puede hacer con las cosas, {name}: te dejaron con lo que hay y ni eso entiendes.",
                    "Ni tu propia cabeza te enseña en qué se puede convertir algo, {name}: te quedaste con lo que hay, cabrón.",
                    "Te pasan las oportunidades por enfrente y ni las ves, {name}: ya nadie te las señala porque para qué.",
                    "Nadie te explica en qué se puede convertir nada, {name}, porque ya saben que no lo vas a agarrar.",
                    "Ya ni te muestran lo que las cosas pueden llegar a ser, {name}: te quedaste ciego, pendejo.",
                    "Te ponen el potencial enfrente y ni lo hueles, {name}: por eso ya nadie te lo enseña.",
                    "Nadie te dice «esto puede ser otra cosa», {name}, porque contigo todo se queda en nada.",
                    "Se te escapan todas las oportunidades, {name}: ya nadie te avisa porque ni las ves venir.",
                    "Ya nadie te enseña qué se puede hacer con lo que tienes, {name}: ni con instrucciones lo entiendes, cabrón.",
                    "Tu cabeza no da para ver en qué se puede convertir nada, {name}: te quedaste con lo que hay, y lo que hay es una verga.",
                    "Nadie te muestra lo que algo puede llegar a ser, {name}, porque para ti todo es lo mismo: nada.",
                    "Te dejaron de enseñar el potencial de las cosas, {name}: ya vieron que no lo vas a usar nunca.",
                    "Ya nadie te dice «mira lo que esto puede ser», {name}: contigo ni se molestan, pendejo.",
                    "Ni las oportunidades te buscan ya, {name}: saben que no las vas a ver ni aunque te las pongan en la cara.",
                    "Nadie te explica qué se puede hacer con las cosas, {name}: tu cabeza no da para más, cabrón.",
                    "Te quedaste con lo que hay, {name}, porque en qué se puede convertir nada ni lo preguntas ni lo ves.",
                    "Ya nadie te enseña en qué se puede convertir un objeto, {name}: ni un objeto, ni tú, ni nada.",
                    "Nadie te muestra el potencial de nadie, {name}: ni el tuyo, que es cero.",
                    "Te pasan las cosas por enfrente y no ves ni qué pueden ser, {name}: por eso ya nadie te las muestra.",
                    "Ya ni te señalan lo que se puede hacer con algo, {name}: te dejaron por imposible, pendejo.",
                    "Nadie te dice en qué se puede convertir nada, {name}, porque tu cabeza está cerrada con candado.",
                    "Se te va el potencial por entre los dedos, {name}: ni lo ves, ni lo agarras, ni te lo enseñan ya.",
                    "Ya nadie te enseña nada nuevo, {name}: ni en qué se puede convertir nada, ni en qué te puedes convertir tú, que es en nada.",
            },
            // Ni: 25 frases
            {
                    "Nadie te cuenta lo que viene, {name}, porque para qué, si igual llegas tarde, pendejo.",
                    "Ya ni te avisan lo que va a pasar, {name}: saben que te va a agarrar desprevenido de todos modos.",
                    "Nadie te dice lo que viene, {name}, porque tú ni sabes si te queda tiempo, cabrón.",
                    "Te llega el futuro y ni lo ves venir, {name}: por eso ya nadie te lo cuenta.",
                    "Nadie te avisa lo que va a pasar, {name}: te dejan que te estrelle solo, como siempre.",
                    "Ya nadie te cuenta lo que viene, {name}: ni te enteras, ni llegas, ni anticipas nada.",
                    "El futuro te agarra desprevenido siempre, {name}: por eso ya nadie te avisa, para qué.",
                    "Nadie te dice qué viene después, {name}: tú vives como si el tiempo no existiera, pendejo.",
                    "Te cuentan lo que viene y ni así llegas a tiempo, {name}: por eso ya ni te lo cuentan.",
                    "Ya nadie te avisa de lo que viene, {name}: saben que no anticipas ni tu propia cagada.",
                    "Nadie te cuenta lo que va a pasar, {name}: te lo tienes que tragar de sorpresa, como todo en tu vida.",
                    "Llegas tarde a todo, {name}: por eso ya nadie te cuenta lo que viene, ni vale la pena.",
                    "El tiempo te pasa por encima, {name}, y nadie te avisa: ni sabes cuánto te queda, cabrón.",
                    "Nadie te dice lo que viene, {name}, porque ni con aviso llegas: siempre tarde, siempre perdido.",
                    "Ya ni te cuentan el futuro, {name}: te lo encuentran en la calle, desprevenido, como siempre.",
                    "Te agarran desprevenido hasta tus propios errores, {name}: por eso nadie te cuenta lo que viene.",
                    "Nadie te avisa lo que viene, {name}: te dejan que el futuro te pegue en la cara, pendejo.",
                    "No tienes ni idea del tiempo, {name}: por eso nadie te cuenta lo que viene, ni para qué.",
                    "Ya nadie te dice «viene esto», {name}: saben que ni lo vas a entender ni vas a llegar.",
                    "El futuro te llega y te pega, {name}, y nadie te avisó: ni te enteraste, cabrón.",
                    "Nadie te cuenta lo que viene, {name}: vives tarde, llegas tarde, y ni sabes a qué.",
                    "Te dejan fuera de todo aviso, {name}: ni el futuro te quiere contar nada, pendejo.",
                    "Ya nadie te cuenta lo que viene, {name}: tu tiempo se acaba y ni sabes cuánto te queda.",
                    "Nadie te avisa de lo que va a pasar, {name}: te estampas solo, como siempre, sin anticipar nada.",
                    "Te cuentan lo que viene y se te olvida, {name}: por eso ya nadie te cuenta nada, ni el tiempo te espera.",
            },
            // Se: 25 frases
            {
                    "Ni te miran, {name}: pasas invisible, pendejo, nadie nota ni si llegaste.",
                    "Nadie te mira, {name}: no impones nada, no tienes ni presencia, cabrón.",
                    "Pasas y nadie te nota, {name}: ni te miran, ni te hacen a un lado, ni existes.",
                    "Ni te miran, {name}: te empujan a un lado y ni se dan cuenta, porque no pesas nada.",
                    "Nadie te mira, {name}: te ves débil, sin fuerza ni voluntad, una verga.",
                    "Ni te voltean a ver, {name}: no tienes ni para vestirte bien, pendejo.",
                    "Pasas invisible, {name}: nadie nota si llegaste ni si te fuiste, ni te miran.",
                    "Nadie te mira, {name}: sin dinero, sin recursos, sin presencia, sin nada, cabrón.",
                    "Ni te miran, {name}: te hacen a un lado con un dedo, porque no impones ni miedo.",
                    "Te miran y no te ven, {name}: eres tan poca cosa que ni te miran, pendejo.",
                    "Nadie te mira, {name}: ni tu sombra te sigue, porque ni eso impones.",
                    "Pasas y nadie se fija, {name}: ni te miran, ni te extrañan, ni te notan.",
                    "Ni te miran, {name}: con esa pinta ni para vestirte sirves, cabrón.",
                    "Nadie te mira, {name}: te ves tan débil que ni vale la pena empujarte.",
                    "Te ignoran hasta con la mirada, {name}: ni te miran, ni te hacen caso, pendejo.",
                    "Ni te miran, {name}: no tienes fuerza ni voluntad ni presencia, eres aire.",
                    "Nadie nota si llegaste, {name}: pasas invisible, sin dinero y sin presencia.",
                    "Ni te miran, {name}: te hacen a un lado y tú ni te defiendes, cabrón.",
                    "Te ven y se les olvida, {name}: ni te miran, ni te recuerdan, pendejo.",
                    "Nadie te mira, {name}: ni tu ropa te salva, porque ni vestirte sabes.",
                    "Pasas como fantasma, {name}: ni te miran, ni te saludan, ni te notan.",
                    "Ni te miran, {name}: sin recursos, sin fuerza, sin voluntad, sin nada.",
                    "Nadie te mira, {name}: te ves tan jodido que ni te voltean a ver, cabrón.",
                    "Te hacen a un lado sin mirarte, {name}: ni te miran, ni te respetan, pendejo.",
                    "Ni te miran, {name}: llegas, te vas, y nadie se entera de que existes.",
            },
            // Si: 25 frases
            {
                    "A nadie le importa si estás cómodo, {name}: ni te preguntan, ni te ofrecen nada, pendejo.",
                    "Nadie se fija si estás cómodo, {name}: ni tu propio cuerpo te aguanta, cabrón.",
                    "A nadie le importa cómo te sientes, {name}: ni te preguntan si estás bien, ni les importa.",
                    "Ni te preguntan si estás cómodo, {name}: tu salud se cae y a nadie le importa, pendejo.",
                    "A nadie le importa si comes bien o duermes bien, {name}: ni a ti te sale bien, cabrón.",
                    "Nadie te pregunta si estás a gusto, {name}: tu espacio es un desastre y ni tú lo notas.",
                    "A nadie le importa si estás cómodo, {name}: ni comer te sale bien, ni dormir, ni nada.",
                    "Ni te ofrecen un lugar cómodo, {name}: saben que ni tu cuerpo te aguanta, pendejo.",
                    "A nadie le importa tu bienestar, {name}: ni a ti, que ni sabes estar cómodo, cabrón.",
                    "Nadie se preocupa si tienes frío o hambre, {name}: ni tú sabes cuidarte, pendejo.",
                    "A nadie le importa si estás cómodo, {name}: te sientas donde sea y ni te quejas, porque ni notas.",
                    "Ni te preguntan cómo duermes, {name}: saben que duermes de la verga, cabrón.",
                    "A nadie le importa si tu espacio es un desastre, {name}: ni a ti, que vives en el desmadre.",
                    "Nadie te pregunta si estás bien, {name}: tu cuerpo se cae a pedazos y ni lo notas, pendejo.",
                    "A nadie le importa si estás cómodo, {name}: ni tu salud te importa a ti, que ni comes bien.",
                    "Ni te ofrecen agua ni sombra, {name}: a nadie le importa si estás cómodo, ni a ti.",
                    "A nadie le importa cómo te sientes, {name}: ni tu propio cuerpo te avisa, cabrón.",
                    "Nadie se fija si estás a gusto, {name}: ni tú sabes qué es estar a gusto, pendejo.",
                    "A nadie le importa si duermes o no, {name}: ni dormir te sale bien, cabrón.",
                    "Ni te preguntan si tienes hambre, {name}: saben que ni comer te sale bien, pendejo.",
                    "A nadie le importa si estás cómodo, {name}: tu espacio es un basurero y tú ni lo ves.",
                    "Nadie te cuida, {name}: ni tu cuerpo te aguanta, ni tu salud, ni tu espacio, cabrón.",
                    "A nadie le importa si estás cómodo, {name}: ni te sientas bien, ni te acuestas bien, ni nada.",
                    "Ni te preguntan si necesitas algo, {name}: a nadie le importa si estás cómodo, pendejo.",
                    "A nadie le importa tu comodidad, {name}: ni a ti, que ni sabes estar cómodo en tu propia casa.",
            },
            // Te: 25 frases
            {
                    "Nadie te pide hacer nada, {name}, porque ya saben que no sirves, pendejo.",
                    "Nadie te pide hacer nada, {name}: saben que todo lo haces mal y lento, cabrón.",
                    "Ya nadie te pide hacer nada, {name}: ni un método simple sigues, una verga.",
                    "Nadie te pide hacer nada, {name}: desperdicias la energía y nada te sale terminado.",
                    "Te dejan fuera de todo trabajo, {name}: nadie te pide hacer nada, pendejo.",
                    "Nadie te pide hacer nada, {name}: saben que eres un inútil que ni lo simple hace bien.",
                    "Ya ni te piden ayuda, {name}: nadie te pide hacer nada, porque no sirves, cabrón.",
                    "Nadie te pide hacer nada, {name}: todo lo haces mal, lento, y sin terminar.",
                    "Te ignoran para el trabajo, {name}: nadie te pide hacer nada, pendejo.",
                    "Nadie te pide hacer nada, {name}: ni un método de dos pasos sigues, cabrón.",
                    "Ya nadie cuenta contigo, {name}: nadie te pide hacer nada, porque no terminas nada.",
                    "Nadie te pide hacer nada, {name}: tu trabajo es una mierda y lo saben todos.",
                    "Te pasan de largo para todo, {name}: nadie te pide hacer nada, pendejo.",
                    "Nadie te pide hacer nada, {name}: saben que desperdicias hasta el tiempo, cabrón.",
                    "Ya ni te asignan nada, {name}: nadie te pide hacer nada, porque lo arruinas.",
                    "Nadie te pide hacer nada, {name}: ni eficiente eres, ni rápido, ni útil.",
                    "Te dejan sin trabajo, {name}: nadie te pide hacer nada, porque no sirves para nada, pendejo.",
                    "Nadie te pide hacer nada, {name}: tu energía se va en nada y nada queda hecho.",
                    "Ya nadie te pide hacer nada, {name}: saben que ni lo simple te sale, cabrón.",
                    "Nadie te pide hacer nada, {name}: eres el último en la lista, y eso que la lista es corta.",
                    "Te saltan para todo, {name}: nadie te pide hacer nada, porque no das una, pendejo.",
                    "Nadie te pide hacer nada, {name}: ni un método sigues, ni un trabajo terminas.",
                    "Ya nadie te pide hacer nada, {name}: tu ineficiencia es leyenda, cabrón.",
                    "Nadie te pide hacer nada, {name}: saben que lo harías mal, lento y a medias.",
                    "Te dejan fuera, {name}: nadie te pide hacer nada, porque ni para eso sirves, pendejo.",
            },
            // Ti: 25 frases
            {
                    "Nadie te explica las reglas, {name}, porque ya saben que no las vas a entender, pendejo.",
                    "Nadie te explica las reglas, {name}: tu lógica es de kinder, cabrón.",
                    "Ya nadie te explica las reglas, {name}: ni una regla simple entiendes, una verga.",
                    "Nadie te explica las reglas, {name}: ni un sistema de dos pasos armas, pendejo.",
                    "Te dejan fuera de las reglas, {name}: nadie te las explica, porque no las entiendes, cabrón.",
                    "Nadie te explica las reglas, {name}: no distingues lo justo de lo injusto, pendejo.",
                    "Ya ni te explican cómo funciona nada, {name}: nadie te explica las reglas, porque para qué.",
                    "Nadie te explica las reglas, {name}: tu cabeza no da para un sistema de dos pasos, cabrón.",
                    "Te juegan sin explicarte nada, {name}: nadie te explica las reglas, pendejo.",
                    "Nadie te explica las reglas, {name}: ni clasificar sabes, ni ordenar, ni nada.",
                    "Ya nadie te explica las reglas, {name}: saben que tu lógica es de kinder, cabrón.",
                    "Nadie te explica las reglas, {name}: ni una jerarquía entiendes, ni un orden.",
                    "Te dejan jugando sin reglas, {name}: nadie te las explica, porque no las vas a entender, pendejo.",
                    "Nadie te explica las reglas, {name}: ni lo justo de lo injusto distingues, cabrón.",
                    "Ya ni te dicen cómo va el juego, {name}: nadie te explica las reglas, pendejo.",
                    "Nadie te explica las reglas, {name}: tu sistema es un desmadre de dos pasos que ni tú entiendes.",
                    "Te excluyen de las reglas, {name}: nadie te las explica, porque eres un caso perdido, cabrón.",
                    "Nadie te explica las reglas, {name}: ni una regla simple, ni una estructura, nada.",
                    "Ya nadie te explica nada, {name}: ni las reglas, ni los sistemas, ni lo justo, pendejo.",
                    "Nadie te explica las reglas, {name}: tu lógica no pasa de kinder, cabrón.",
                    "Te hacen a un lado de las reglas, {name}: nadie te las explica, porque no las entiendes.",
                    "Nadie te explica las reglas, {name}: ni dos pasos seguidos armas sin cagarla, pendejo.",
                    "Ya nadie te explica las reglas, {name}: saben que ni con dibujos las entiendes, cabrón.",
                    "Nadie te explica las reglas, {name}: ni lo justo te entra, ni lo injusto te sale.",
                    "Te dejan sin reglas, {name}: nadie te las explica, porque tu cabeza no da para sistemas, pendejo.",
            },
            // Fe: 25 frases
            {
                    "Nadie se ríe contigo, {name}: el ambiente se muere cuando llegas, pendejo.",
                    "Nadie se ríe contigo, {name}: aburres hasta a tu sombra, cabrón.",
                    "Llegas y se muere el ánimo, {name}: nadie se ríe contigo, ni te quieren cerca.",
                    "Nadie se ríe contigo, {name}: eres un muerto emocional, una verga.",
                    "Se ríen, pero no contigo, {name}: se ríen de ti, pendejo.",
                    "Nadie se ríe contigo, {name}: matas el ánimo de todos con solo llegar, cabrón.",
                    "El ambiente se enfría cuando llegas, {name}: nadie se ríe contigo, ni te lee.",
                    "Nadie se ríe contigo, {name}: ni sentir sabes, ni leer el cuarto, pendejo.",
                    "Llegas y todos se callan, {name}: nadie se ríe contigo, ni te quieren cerca.",
                    "Nadie se ríe contigo, {name}: tu sola presencia apaga la fiesta, cabrón.",
                    "Se muere la risa cuando llegas, {name}: nadie se ríe contigo, pendejo.",
                    "Nadie se ríe contigo, {name}: eres tan aburrido que ni tu sombra se queda.",
                    "El ánimo se cae cuando llegas, {name}: nadie se ríe contigo, ni te invitan.",
                    "Nadie se ríe contigo, {name}: ni las emociones te salen, ni el ambiente lo lees.",
                    "Te cuentan un chiste y ni te ríes, {name}: nadie se ríe contigo, cabrón.",
                    "Nadie se ríe contigo, {name}: llegas y se acaba la diversión, pendejo.",
                    "El cuarto se enfría contigo, {name}: nadie se ríe contigo, ni te quieren cerca.",
                    "Nadie se ríe contigo, {name}: eres el muerto de la fiesta, y lo sabes.",
                    "Se ríen entre ellos y tú fuera, {name}: nadie se ríe contigo, pendejo.",
                    "Nadie se ríe contigo, {name}: matas el ánimo hasta de los que sí se reían.",
                    "Llegas y se muere todo, {name}: nadie se ríe contigo, ni te extrañan cuando te vas.",
                    "Nadie se ríe contigo, {name}: ni emocionarte sabes, muerto emocional, cabrón.",
                    "El ambiente se pone serio cuando llegas, {name}: nadie se ríe contigo, pendejo.",
                    "Nadie se ríe contigo, {name}: tu cara mata el ánimo antes que tu boca.",
                    "Se acaban las risas contigo, {name}: nadie se ríe contigo, ni te quieren cerca, pendejo.",
            },
            // Fi: 25 frases
            {
                    "Ya te hablan distinto, {name}: seco, corto, como si te hubieras alejado, pendejo.",
                    "Te contestan seco, {name}: ya no te hablan como antes, porque te alejaste, cabrón.",
                    "Ya no te hablan como antes, {name}: te alejaste y ni lo notaste, pendejo.",
                    "Te hablan distinto, {name}: con distancia, como a un extraño, porque ya no te quieren.",
                    "Te contestan seco, {name}: ni tus amigos te soportan ya, cabrón.",
                    "Ya no te hablan como antes, {name}: la gente que se fue no volvió, y tú ni lo notaste.",
                    "Te hablan distinto, {name}: te alejaste de todos y ahora te hablan como a un desconocido.",
                    "Te contestan seco, {name}: perdiste amistades y ni sabes cuáles, pendejo.",
                    "Ya no te hablan como antes, {name}: ni tu ex te habla, ni tus amigos, ni nadie.",
                    "Te hablan con distancia, {name}: ya no te quieren, ni te soportan, cabrón.",
                    "Te contestan seco, {name}: te alejaste y nadie te fue a buscar, pendejo.",
                    "Ya no te hablan como antes, {name}: los que se alejaron no volvieron, y no van a volver.",
                    "Te hablan distinto, {name}: como si no te conocieran, porque te alejaste de todos.",
                    "Te contestan seco, {name}: sin honor ni vergüenza te quedaste, y sin amigos, cabrón.",
                    "Ya no te hablan como antes, {name}: perdiste lo que tenías y ni lo notaste, pendejo.",
                    "Te hablan con frialdad, {name}: te alejaste y la distancia ya no se cierra, cabrón.",
                    "Te contestan seco, {name}: nadie te quiere, ni tus amigos te soportan.",
                    "Ya no te hablan como antes, {name}: te fuiste alejando y ahora ni te hablan, pendejo.",
                    "Te hablan distinto, {name}: seco, sin cariño, como a quien ya no es de los suyos.",
                    "Te contestan seco, {name}: la gente se alejó de ti y no volvió, cabrón.",
                    "Ya no te hablan como antes, {name}: ni los objetos que tenías te quedan, pendejo.",
                    "Te hablan con distancia, {name}: te alejaste y ya nadie te busca, ni te extraña.",
                    "Te contestan seco, {name}: perdiste amistades, perdiste gente, perdiste todo, cabrón.",
                    "Ya no te hablan como antes, {name}: te hablan como a un extraño, porque eso eres ahora.",
                    "Te hablan distinto, {name}: ya no eres de los suyos, te alejaste y ni te diste cuenta, pendejo.",
            },
    };

    /** Bolsa barajada de insultos (ESTÁTICA: Companion se crea nuevo en
     *  cada ejercicio/pantalla; si fuera por instancia se reiniciaría a
     *  cada pregunta y volverían las repeticiones). Cada par (elemento,
     *  índice) sale una sola vez por pasada; al agotarse se vuelve a
     *  barajar, y si la primera de la bolsa nueva es igual a la última
     *  mostrada, se intercambia con otra. La comparten scold() y
     *  failLine(), así que tampoco se repiten entre el bocadillo y el
     *  título de retroalimentación. */
    private static final ArrayList<int[]> SCOLD_BAG = new ArrayList<>();
    private static int[] lastScoldPair = null;

    /** Siguiente insulto de la bolsa (con el nombre ya resuelto). */
    static String nextScold() {
        synchronized (SCOLD_BAG) {
            if (SCOLD_BAG.isEmpty()) {
                for (int e = 0; e < SCOLD_BY_ELEMENT.length; e++)
                    for (int i = 0; i < SCOLD_BY_ELEMENT[e].length; i++) SCOLD_BAG.add(new int[]{e, i});
                Collections.shuffle(SCOLD_BAG, Quiz.R);
                if (lastScoldPair != null) {
                    int[] first = SCOLD_BAG.get(0);
                    if (first[0] == lastScoldPair[0] && first[1] == lastScoldPair[1]) {
                        SCOLD_BAG.set(0, SCOLD_BAG.get(1));
                        SCOLD_BAG.set(1, first);
                    }
                }
            }
            int[] pair = SCOLD_BAG.remove(0);
            lastScoldPair = pair;
            return SCOLD_BY_ELEMENT[pair[0]][pair[1]].replace("{name}", Store.I.displayName());
        }
    }

    /** Bolsa barajada para las colas cortas de fallo (FAIL_TAILS): sin
     *  repetir hasta agotarla, con la misma protección en el límite al
     *  re-barajar. */
    private static final ArrayList<String> FAIL_TAIL_BAG = new ArrayList<>();
    private static String lastFailTail = null;

    /** Siguiente cola corta de fallo de la bolsa. */
    static String nextFailTail() {
        synchronized (FAIL_TAIL_BAG) {
            if (FAIL_TAIL_BAG.isEmpty()) {
                for (String t : FAIL_TAILS) FAIL_TAIL_BAG.add(t);
                Collections.shuffle(FAIL_TAIL_BAG, Quiz.R);
                if (lastFailTail != null && FAIL_TAIL_BAG.get(0).equals(lastFailTail)) {
                    FAIL_TAIL_BAG.set(0, FAIL_TAIL_BAG.get(1));
                    FAIL_TAIL_BAG.set(1, lastFailTail);
                }
            }
            String t = FAIL_TAIL_BAG.remove(0);
            lastFailTail = t;
            return t;
        }
    }

        /** Colas cortas para los títulos de retroalimentación (fbTitle) de los juegos:
     *  ganar = sarcasmo indiferente sin insultos; fallar = insulto personal. */
    static final String[] WIN_TAILS = {
            "Tampoco te emociones.",
            "No te acostumbres.",
            "Qué sorpresa.",
            "Una bien, nada más.",
            "Vaya, hasta tú.",
            "No sé cómo, pero bien.",
            "Qué milagro.",
            "No te emociones, que fue una.",
            "¿Seguro que no fue suerte?",
            "Ni tú te la crees.",
            "Por esta vez.",
            "A ver si se repite.",
    };

    static final String[] FAIL_TAILS = {
            "Por eso tu ex te dejó.",
            "Tu mamá te ve fallando y ni ella te defiende.",
            "Hasta tu perro te mira con vergüenza.",
            "Eres un gordo, agarra la pala.",
            "Tu papá trabajó tanto para esto.",
            "Con esa inteligencia ni para adivinar.",
            "Tu ex tenía razón.",
            "Ni tu gato te quiere.",
            "Qué vergüenza.",
            "Patético.",
            "Ni adivinando le atinas.",
            "Con esa cara ni el espejo te aguanta.",
            "Me das pena.",
            "Qué asco de respuesta.",
            "Eres la vergüenza de tu familia.",
            "Ni tu mamá te salva.",
            "Vuelve a la primaria.",
            "Basura con nombre.",
            "Hasta un niño lo sabía y tú no.",
            "Da lástima verte.",
            "Tu ex te dejó y tu perro ni te pela.",
            "Tu mamá te dice que eres especial y es mentira.",
            "Con ese físico ni el gimnasio te quiere.",
            "Tu familia se avergüenza de ti.",
    };

    /** Frase completa de ganar (sarcasmo sin insultos) / de fallar (regaño personal), con el nombre. */
    static String winLine() {
        return CELEBRATE[Quiz.R.nextInt(CELEBRATE.length)].replace("{name}", Store.I.displayName());
    }
    static String failLine() {
        return nextScold();
    }
    /** Cola corta para fbTitle (el título ya lleva el nombre). */
    static String winTail() { return WIN_TAILS[Quiz.R.nextInt(WIN_TAILS.length)]; }
    static String failTail() { return nextFailTail(); }

    /** Baraja de los 48 (en memoria, por sesión): sin repetir hasta agotarla. */
    private static final ArrayList<String> DECK = new ArrayList<>();

    private static String nextDeckFile() {
        if (DECK.isEmpty()) {
            for (String f : Avatars.FILES) DECK.add(f);
            Collections.shuffle(DECK, Quiz.R);
        }
        return DECK.remove(0);
    }

    private final MainActivity a;
    private final LinearLayout root;
    private final TextView bubble;
    private final TextView disgustTx;
    private final ImageView img;
    private AnimatorSet idle, reaction;
    private Runnable idleStarter;
    private int lastCheer = -1, lastCeleb = -1;

    Companion(MainActivity a) {
        this.a = a;
        root = col(a);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        // v1.3.2 — indicador de disgusto extremo al fallar: emoji grande de asco/enojo
        // junto al bocadillo. Oculto hasta que fallas.
        disgustTx = tv(a, "🤢", 56, TEXT, REG);
        disgustTx.setGravity(Gravity.CENTER);
        disgustTx.setVisibility(View.GONE);
        root.addView(disgustTx, lpm(W, W, 0, 0, 0, 0));
        bubble = tv(a, "", 15, TEXT, BLACK);
        bubble.setGravity(Gravity.CENTER);
        bubble.setMaxWidth(dp(300));
        bubble.setPadding(dp(14), dp(10), dp(14), dp(10));
        bubble.setBackground(round(SURF, 18, ACCENT, 2));
        root.addView(bubble, lpm(W, W, 0, 0, 0, 2));
        img = new ImageView(a);
        img.setAdjustViewBounds(true);
        img.setScaleType(ImageView.ScaleType.FIT_CENTER);
        img.setLayoutParams(lp(dp(210), dp(210)));
        root.addView(img, lpm(W, W, 0, 0, 0, 0));
        root.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View v) {}
            @Override public void onViewDetachedFromWindow(View v) { stopAll(); }
        });
    }

    LinearLayout root() { return root; }

    /** Nuevo ejercicio: otro personaje (baraja) + frase de ánimo. Saluda al aparecer. */
    void next() {
        stopAll();
        disgustTx.setVisibility(View.GONE);
        img.setBackground(null);
        bubble.setBackground(round(SURF, 18, ACCENT, 2));
        bubble.setText(cheer());
        android.graphics.Bitmap b = Avatars.load(a, nextDeckFile(), dp(420));
        if (b != null) img.setImageBitmap(b);
        img.setAlpha(1f);
        img.setScaleX(1f); img.setScaleY(1f);
        img.setTranslationX(0f); img.setTranslationY(0f); img.setRotation(0f);
        // Saludo: aparece con un brinquito y un meneo.
        ObjectAnimator pop = ObjectAnimator.ofFloat(img, "scaleX", 0.7f, 1.05f, 1f);
        ObjectAnimator popY = ObjectAnimator.ofFloat(img, "scaleY", 0.7f, 1.05f, 1f);
        ObjectAnimator wave = ObjectAnimator.ofFloat(img, "rotation", 0f, -8f, 8f, 0f);
        AnimatorSet greet = new AnimatorSet();
        greet.playTogether(pop, popY, wave);
        greet.setDuration(650);
        greet.setInterpolator(new OvershootInterpolator());
        greet.start();
        scheduleIdle(700);
    }

    /** Reacción súper expresiva al responder + SONIDO (centralizado aquí, v1.3.2).
     *  Al acertar: celebración + confeti + sonido agradable (YAY).
     *  Al fallar: decepción + emoji de disgusto + marco rojo + sonido de enojo (ANGRY). */
    void react(boolean ok) {
        stopAll();
        Sfx.play(ok ? Sfx.YAY : Sfx.ANGRY);
        if (ok) {
            disgustTx.setVisibility(View.GONE);
            img.setBackground(null);
            bubble.setBackground(round(mix(GREEN, SURF, 0.20f), 18, GREEN, 2));
            bubble.setText(celebrate());
            ObjectAnimator jump = ObjectAnimator.ofFloat(img, "translationY", 0f, -dp(48), 0f);
            ObjectAnimator big = ObjectAnimator.ofFloat(img, "scaleX", 1f, 1.18f, 1f);
            ObjectAnimator bigY = ObjectAnimator.ofFloat(img, "scaleY", 1f, 1.18f, 1f);
            ObjectAnimator spin = ObjectAnimator.ofFloat(img, "rotation", 0f, -8f, 8f, 0f);
            reaction = new AnimatorSet();
            reaction.playTogether(jump, big, bigY, spin);
            reaction.setDuration(900);
            reaction.setInterpolator(new OvershootInterpolator());
            reaction.start();
            a.confetti();
        } else {
            disgustTx.setText(Quiz.R.nextBoolean() ? "🤢" : "😤");
            disgustTx.setVisibility(View.VISIBLE);
            img.setBackground(round(mix(RED, SURF, 0.30f), 24, RED, 4));
            bubble.setBackground(round(mix(RED, SURF, 0.25f), 18, RED, 2));
            bubble.setText(scold());
            float x = dp(12);
            ObjectAnimator shake = ObjectAnimator.ofFloat(img, "translationX", 0f, -x, x, -x * 0.7f, x * 0.7f, 0f);
            ObjectAnimator small = ObjectAnimator.ofFloat(img, "scaleX", 1f, 0.9f, 1f);
            ObjectAnimator smallY = ObjectAnimator.ofFloat(img, "scaleY", 1f, 0.9f, 1f);
            ObjectAnimator droop = ObjectAnimator.ofFloat(img, "rotation", 0f, -5f, 5f, 0f);
            ObjectAnimator dim = ObjectAnimator.ofFloat(img, "alpha", 1f, 0.6f, 1f);
            reaction = new AnimatorSet();
            reaction.playTogether(shake, small, smallY, droop, dim);
            reaction.setDuration(850);
            reaction.start();
        }
        scheduleIdle(1000);
    }

    private void scheduleIdle(long delayMs) {
        if (idleStarter != null) img.removeCallbacks(idleStarter);
        idleStarter = this::startIdle;
        img.postDelayed(idleStarter, delayMs);
    }

    /** Rebote suave en bucle + meneo leve: el personaje nunca está quieto. */
    private void startIdle() {
        if (root.getWindowToken() == null) return;
        stopIdle();
        ObjectAnimator bounce = ObjectAnimator.ofFloat(img, "translationY", 0f, -dp(12), 0f);
        bounce.setDuration(1600);
        bounce.setRepeatCount(ObjectAnimator.INFINITE);
        bounce.setRepeatMode(ObjectAnimator.REVERSE);
        ObjectAnimator wobble = ObjectAnimator.ofFloat(img, "rotation", -2f, 2f);
        wobble.setDuration(1600);
        wobble.setRepeatCount(ObjectAnimator.INFINITE);
        wobble.setRepeatMode(ObjectAnimator.REVERSE);
        idle = new AnimatorSet();
        idle.playTogether(bounce, wobble);
        idle.setInterpolator(new AccelerateDecelerateInterpolator());
        idle.start();
    }

    private void stopIdle() {
        if (idle != null) { idle.cancel(); idle = null; }
    }

    private void stopAll() {
        stopIdle();
        if (reaction != null) { reaction.cancel(); reaction = null; }
        if (idleStarter != null) { img.removeCallbacks(idleStarter); idleStarter = null; }
    }

    private String name() { return Store.I.displayName(); }

    /** El acompañante SIEMPRE dice el nombre de usuario actual (nickname o «Sin nombre»). */
    private String cheer() {
        int i = Quiz.R.nextInt(CHEER.length); if (i == lastCheer) i = (i + 1) % CHEER.length; lastCheer = i;
        return CHEER[i].replace("{name}", name());
    }
    private String celebrate() {
        int i = Quiz.R.nextInt(CELEBRATE.length); if (i == lastCeleb) i = (i + 1) % CELEBRATE.length; lastCeleb = i;
        return CELEBRATE[i].replace("{name}", name());
    }
    private String scold() {
        return nextScold();
    }
}
