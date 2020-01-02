package org.futurerobotics.botsystem

import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.ArrayList

/**
 * A way for organizing subsystems, based off of _dependencies_ and running using coroutines.
 *
 * This by itself isn't much but a container, it is the [Element]s that do all the work.
 */
interface BotSystem {

    /**
     * The coroutine scope of this bot system, in which all elements are launched from.
     */
    val scope: CoroutineScope
    /**
     * The job of this bot system, in which all running element processes are a child of.
     */
    @JvmDefault
    val job: Job
        get() = scope.coroutineContext[Job]!!

    /** A collection of all elements. */
    val elements: Collection<Element>

    /**
     * Get an [Element] via its [identifier class][Element.identifierClass], or throws an exception if
     * it does not exist.
     *
     * @see [elements]
     */
    fun <S : Element> get(identifier: Class<S>): S

    /**
     * Attempts to get an [Element] via its [identifier class][Element.identifierClass], or `null`
     * it does not exist.
     *
     * @see [elements]
     */
    @Suppress("UNCHECKED_CAST")
    fun <S : Element> tryGet(identifier: Class<S>): S?

    /**
     * Initializes all elements.
     *
     * If [usingScope] is true, the elements will be initialized using coroutines launched from this bot
     * system's [coroutineScope].
     */
    @Throws(InterruptedException::class)
    fun initBlocking(usingScope: Boolean = true): Unit = runBlocking {
        init(usingScope)
    }

    /**
     * Initializes all elements.
     *
     * If [usingScope] is true, the elements will be initialized using coroutines launched from this bot
     * system's [coroutineScope].
     */
    @JvmSynthetic
    suspend fun init(usingScope: Boolean = true)

    /**
     * Calls [Element.start] on all [Element]s.
     *
     * This is nothing more than a message to the elements.
     */
    fun start()

    /**
     * If is started.
     */
    val isStarted: Boolean

    /**
     * Waits until `start` has been called and after all Elements have [BotSystem.start] called.
     */
    suspend fun waitForStart()

    /**
     * Waits until `start` has been called, blocking.
     */
    @JvmDefault
    @Throws(InterruptedException::class)
    fun waitForStartBlocking() = runBlocking {
        waitForStart()
    }

    /**
     * Stops the system by way of cancelling all children of the system.
     */
    fun stop()

    /**
     * Awaits all elements to be done by waiting on the coroutineScope.
     */
    @JvmSynthetic
    suspend fun awaitTermination()

    /**
     * Awaits all elements to be done by waiting the coroutineScope.
     */
    @Throws(InterruptedException::class)
    fun awaitTerminationBlocking() = runBlocking {
        awaitTermination()
    }

    companion object {
        /**
         * Creates a new [BotSystem] with the given [elements].
         *
         * A [scope] must also be supplied.
         *
         * ***A mechanism such that thrown exceptions are handled in some way is strongly recommended if you don't
         * want the entire app to crash if an exception happens.***
         * This can be done by:
         * - If the [scope] is a top-level coroutine, a [CoroutineExceptionHandler] can be installed.
         * - The [scope] contains a child job, so the exceptions have somewhere a parent to propagate to in
         *   which the exception can be handled elsewhere.
         */
        @JvmStatic
        fun create(
            scope: CoroutineScope,
            elements: Collection<Element>
        ): BotSystem = BotSystemImpl(scope, elements)

        /** @see [create] */
        @JvmStatic
        fun create(
            scope: CoroutineScope,
            vararg elements: Element
        ): BotSystem = BotSystemImpl(scope, elements.asList())
    }
}


inline fun <reified T : Element> BotSystem.get() = get(T::class.java)
inline fun <reified T : Element> BotSystem.tryGet() = tryGet(T::class.java)


internal class BotSystemImpl(
    override val scope: CoroutineScope,
    initialElements: Collection<Element>
) : BotSystem {

    init {
        requireNotNull(scope.coroutineContext[Job]) { "Coroutine scope must have a job." }
    }

    private val _elements = ArrayList<Element>()

    private val mappedElements = HashMap<Class<out Element>, Element>()

    private var isInited = false


    init {
        val elementsToAdd = mutableMapOf<Any, Element>()
        initialElements.forEach {
            val identifierClass = it.identifierClass
            if (identifierClass == null) {
                elementsToAdd[it] = it
            } else {
                if (identifierClass in elementsToAdd)
                    throw IllegalArgumentException("Cannot have two elements with the same identifierClass")
                elementsToAdd[identifierClass] = it
            }
        }
        val considering = hashSetOf<Class<out Element>>()

        fun addElement(element: Element) {
            val clazz = element.identifierClass
            if (clazz != null) {
                require(clazz.isInstance(element)) { "Element identifier is not a super class of itself" }
                require(clazz !in considering) { "Circular dependency at $clazz" }
                considering += clazz
            }
            element.dependsOn.forEach {
                if (it in mappedElements || it in elementsToAdd) return@forEach
                val defaultElement = Element.tryCreateDefault(it)
                    ?: throw IllegalStateException("Dependency $it does not exit nor have a default creator")
                addElement(defaultElement)
            }
            if (clazz != null) {
                require(clazz.isInstance(element)) { "Element identifier is not a super class of itself" }
                considering -= clazz
                mappedElements[clazz] = element
            }
            _elements += element
        }

        fun <T> MutableIterable<T>.removeFirst(): T =
            iterator().run { next().also { remove() } }
        while (elementsToAdd.isNotEmpty()) {
            addElement(elementsToAdd.values.removeFirst())
        }
    }

    override val elements: Collection<Element> = Collections.unmodifiableCollection(_elements)

    override fun <S : Element> get(identifier: Class<S>): S =
        tryGet(identifier) ?: error("Element with identifier $identifier does not exist.")

    @Suppress("UNCHECKED_CAST")
    override fun <S : Element> tryGet(identifier: Class<S>): S? = mappedElements[identifier] as S?

    override suspend fun init(usingScope: Boolean) {
        if (isInited) throw IllegalStateException("Already initialized!")
        isInited = true
        if (usingScope) {
            scope.launch { doInit() }.join()
        } else {
            doInit()
        }
    }

    private suspend fun doInit() = coroutineScope {
        val clsJobs = ConcurrentHashMap<Class<out Element>, Job>()
        val allJobs = elements.map { el ->
            val cls = el.identifierClass
            launch(start = CoroutineStart.LAZY) {
                el.dependsOn.map { clsJobs[it]!! }.joinAll()
                el.init(this@BotSystemImpl)
            }.also {
                if (cls != null) {
                    clsJobs[cls] = it
                }
            }
        }
        allJobs.forEach {
            it.start()
        }
    }

    private val startedJob = Job(job)

    override fun start() {
        if (!isInited) throw IllegalStateException("Not initialized!")
        elements.forEach(Element::start)
        startedJob.complete()
    }

    override val isStarted: Boolean get() = startedJob.isCompleted

    override suspend fun waitForStart() {
        startedJob.join()
    }


    override fun stop() {
        if (!isInited) return
        scope.cancel()
    }

    override suspend fun awaitTermination() {
        if (!isInited) return
        job.join()
    }
}


