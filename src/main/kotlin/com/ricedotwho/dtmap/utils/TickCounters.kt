package com.ricedotwho.dtmap.utils

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents

object TickCounters {
    fun register() {
        ClientTickEvents.START_CLIENT_TICK.register { ClientTickCounter.tick() }
    }

    enum class Format {
        LINE,
        ROUND
    }

    class Timer(val server: Counter, val client: Counter, var ended: Boolean = false) {
        companion object {
            fun start(): Timer = Timer(ServerTickCounter.start(0), ClientTickCounter.start(0))
            fun zero(): Timer = Timer(Counter.zero(), Counter.zero(), true)
        }

        fun snapshot(): Timer =
            Timer(
                object : Counter(server.amount) { override fun remove() {} },
                object : Counter(client.amount) { override fun remove() {} },
                true
            )

        fun end() {
            if (ended) return
            ended = true
            server.remove()
            client.remove()
        }

        fun format(format: Format = Format.LINE): String {
            return when (format) {
                Format.LINE -> {
                    if (server.amount == 0 && client.amount == 0 && ended) {
                        return "0"
                    }

                    val serverFormatted = String.format("%.2f", server.amount / 20.0f)
                    if (!ended || client.amount != server.amount) {
                        val clientFormatted = String.format("%.2f", client.amount / 20.0f)
                        "$clientFormatted | $serverFormatted"
                    } else {
                        serverFormatted
                    }
                }

                Format.ROUND -> "${String.format("%.2f", client.amount / 20.0f)}s §7(${String.format("%.2f", server.amount / 20.0f)})"
            }
        }
    }

    abstract class CountDown(var amount: Int, val callback: (() -> Unit)?) {
        abstract fun remove()
    }

    abstract class Counter(var amount: Int) {
        companion object {
            fun zero(): Counter =
                object : Counter(0) {
                    override fun remove() {}
                    override fun ticks(): Boolean = false
                }
        }

        abstract fun remove()

        open fun ticks(): Boolean = true
    }

    class TickCounter {
        private val countDowns: MutableList<CountDown> = mutableListOf()
        private val counters: MutableList<Counter> = mutableListOf()

        fun countDown(amount: Int, callback: (() -> Unit)? = null): CountDown {
            val counter = object : CountDown(amount, callback) {
                override fun remove() {
                    synchronized (countDowns) {
                        countDowns.remove(this)
                    }
                }
            }

            synchronized (countDowns) {
                countDowns.add(counter)
            }
            return counter
        }

        fun start(amount: Int = 0): Counter {
            val counter = object : Counter(amount) {
                override fun remove() {
                    synchronized (counters) {
                        counters.remove(this)
                    }
                }
            }

            synchronized (counters) {
                counters.add(counter)
            }

            return counter
        }

        fun tick() {
            synchronized (countDowns) {
                countDowns.forEach { if (--it.amount <= 0 && it.callback != null) it.callback() }
                countDowns.removeIf { it.amount <= 0 }
            }

            synchronized (counters) {
                for (i in 0 until counters.size) {
                    counters[i].amount++
                }
            }
        }
    }

    val ServerTickCounter = TickCounter()
    val ClientTickCounter = TickCounter()
}