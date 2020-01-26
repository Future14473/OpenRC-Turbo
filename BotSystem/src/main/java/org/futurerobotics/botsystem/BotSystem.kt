package org.futurerobotics.botsystem

import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.HashSet
import kotlin.coroutines.EmptyCoroutineContext

/**
 * A way for organizing subsystems, based off of _dependencies_ and running using coroutines.
 *
 * This by itself isn't much but a container, it is the [Element]s that do all the work.
 */
sealed class BotSystem {

    /**
     * The coroutine scope of this bot system, in which all elements are launched from.
     */
    abstract val coroutineScope: CoroutineScope
    /**
     * The job of this bot system, in which all running element processes are a child of.
     */
    val job: Job
        get() = coroutineScope.coroutineContext[Job]!!

    /** A collection of all elements. */
    abstract val elements: Collection<Element>

    /**
     * Gets only ***one*** [Element] via a class, or throws an exception if it does not exist.
     *
     * @see tryGet
     * @see getAll
     */
    abstract fun <E> get(identifier: Class<E>): E

    /**
     * Gets only ***one*** [Element] via a class, or `null` if it does not exist.
     *
     * @see get
     * @see getAll
     */
    @Suppress("UNCHECKED_CAST")
    abstract fun <E> tryGet(identifier: Class<E>): E?

    /**
     * Gets all [Element]s that are a subclass fo the given [identifier].
     *
     * @see get
     * @see tryGet
     */
    abstract fun <E> getAll(identifier: Class<E>): Collection<E>


    @JvmSynthetic
    inline fun <reified T> get() = get(T::class.java)

    @JvmSynthetic
    inline fun <reified T> tryGet() = tryGet(T::class.java)

    @JvmSynthetic
    inline fun <reified T> getAll() = getAll(T::class.java)


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
    abstract suspend fun init(usingScope: Boolean = true)

    /**
     * Calls [Element.start] on all [Element]s.
     *
     * This is nothing more than a message to the elements.
     */
    abstract fun start()

    /**
     * If is started.
     */
    abstract val isStarted: Boolean

    /**
     * Waits until `start` has been called and after all Elements have [BotSystem.start] called.
     */
    abstract suspend fun waitForStart()

    /**
     * Waits until `start` has been called, blocking.
     */
    @Throws(InterruptedException::class)
    fun waitForStartBlocking() = runBlocking {
        waitForStart()
    }

    /**
     * Stops the system by way of cancelling all children of the system.
     */
    abstract fun stop()

    /**
     * Awaits all elements to be done by waiting on the coroutineScope.
     */
    @JvmSynthetic
    abstract suspend fun awaitTermination()

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
            elements: Iterable<Element>
        ): BotSystem = BotSystemImpl(scope, elements)

        /** @see [create] */
        @JvmStatic
        fun create(
            scope: CoroutineScope,
            vararg elements: Element
        ): BotSystem = BotSystemImpl(scope, elements.asList())
    }
}


//Optimize? probably not necessary.
internal class BotSystemImpl(
    override val coroutineScope: CoroutineScope,
    initialElements: Iterable<Element>
) : BotSystem() {

    private val allElements = mutableSetOf<Element>()

    private val mappedElements = mutableMapOf<Class<*>, MutableCollection<Element>>()


    internal val identifiedBy: MutableMap<Element, Set<Class<*>>> = HashMap()

    init {
        initialElements.forEach {
            addToIdentifiedBy(it)
        }
        val elementsToAdd = initialElements.toMutableSet()

        val util = object {
            val considering = HashSet<Class<*>>() //for detecting circular dependencies
            fun resolveDependency(clazz: Class<*>) {
                if (clazz in considering) throw IllegalArgumentException("Circular dependency: $clazz")
                if (clazz in mappedElements) return
                considering += clazz

                var added = false
                elementsToAdd.forEach { toAddElement ->
                    if (clazz.isInstance(toAddElement)) {
                        elementsToAdd -= toAddElement
                        addElement(toAddElement)
                        added = true
                    }
                }
                if (!added) {
                    val defaultElement = Element.tryCreateDefault(clazz)
                        ?: throw IllegalArgumentException("Cannot create default for dependency $clazz")
                    addElement(defaultElement)
                }
                considering -= clazz
            }


            fun addElement(element: Element) {
                element.dependsOn.forEach {
                    resolveDependency(it)
                }
                addToIdentifiedBy(element)
                identifiedBy.getValue(element).forEach {
                    mappedElements.getOrPut(it) { mutableSetOf() } += element
                }
                allElements += element
            }
        }

        fun <T> MutableIterable<T>.removeFirst(): T = iterator().run { next().also { remove() } }

        while (elementsToAdd.isNotEmpty()) {
            util.addElement(elementsToAdd.removeFirst())
        }
    }

    private fun addToIdentifiedBy(element: Element) {
        val elementClass = element.javaClass
        identifiedBy[element] =
            sequence<Class<*>> {
                var curClass: Class<*>? = elementClass
                yieldAll(elementClass.interfaces.asList())
                while (curClass != null) {
                    yield(curClass)
                    curClass = curClass.superclass
                }
            }.toHashSet()
    }

    private var isInited = false


    override val elements: Collection<Element> = Collections.unmodifiableCollection(allElements)

    override fun <E> get(identifier: Class<E>): E =
        tryGet(identifier) ?: error("Element with class $identifier does not exist.")

    @Suppress("UNCHECKED_CAST")
    override fun <E> tryGet(identifier: Class<E>): E? =
        mappedElements[identifier]?.firstOrNull() as E?

    override fun <E> getAll(identifier: Class<E>): Collection<E> {
        @Suppress("UNCHECKED_CAST")
        return mappedElements.getOrElse(identifier) { emptyList<E>() } as Collection<E>
    }

    override suspend fun init(usingScope: Boolean) {
        if (isInited) throw IllegalStateException("Already initialized!")
        isInited = true
        val context = if (usingScope) coroutineScope.coroutineContext else EmptyCoroutineContext
        withContext(context) {
            val allJobs = ConcurrentHashMap<Element, Job>()
            elements.associateWithTo(allJobs) { el ->
                launch(start = CoroutineStart.LAZY) {
                    el.dependsOn.forEach { dep ->
                        mappedElements[dep]!!.forEach { el ->
                            allJobs[el]!!.join()
                        }
                    }
                    el.init(this@BotSystemImpl)
                }
            }
            allJobs.values.forEach {
                it.start()
            }
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
        coroutineScope.cancel()
    }

    override suspend fun awaitTermination() {
        if (!isInited) return
        job.join()
    }
}


