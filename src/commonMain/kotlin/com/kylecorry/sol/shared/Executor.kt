package com.kylecorry.sol.shared

public interface Executor {
    /**
     * Execute tasks that map to a value.
     * @param tasks the tasks
     * @return the evaluated values in the same order as the tasks
     */
    public fun <T> map(tasks: List<() -> T>): List<T>

    /**
     * Execute tasks.
     * @param tasks the tasks
     */
    public fun run(tasks: List<() -> Unit>)
}
