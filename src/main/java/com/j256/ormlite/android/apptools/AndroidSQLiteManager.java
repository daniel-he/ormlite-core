package com.j256.ormlite.android.apptools;

import android.content.Context;

/**
 * There are several schemes to manage the database connections in an Android app, but as an app gets more complicated, there are
 * many potential places where database locks can occur.  This class helps organize database creation and access in a manner that
 *  will allow database connection sharing between multiple processes in a single app.
 *
 * To use this class, you must either call init with an instance of SQLiteOpenHelperFactory, or (more commonly) provide the name of your helper class in
 * @string under 'open_helper_classname'..  The factory simply creates your SQLiteOpenHelper instance.  This will only be called once per app
 * vm instance and kept in a static field.
 *
 * The SQLiteOpenHelper and database classes maintain one connection under the hood, and prevent locks in the java code.
 * Creating mutliple connections can potentially be a source of trouble.  This class shares the same conneciton instance
 * between multiple clients, which will allow multiple activites and services to run at the same time.
 *
 * @author kevingalligan, graywatson
 */
public class AndroidSQLiteManager
{
    private static SQLiteOpenHelperFactory factory;
    private static volatile OrmLiteSQLiteOpenHelper instance;
    private static int instanceCount = 0;

    public static void init(SQLiteOpenHelperFactory factory)
    {
        AndroidSQLiteManager.factory = factory;
    }

    public static OrmLiteSQLiteOpenHelper getInstance(Context context)
    {
        if(factory == null)
        {
            ClassNameProvidedOpenHelperFactory fact = new ClassNameProvidedOpenHelperFactory();
            init(fact);
        }

        if(instance == null)
        {
            synchronized (AndroidSQLiteManager.class)
            {
                //Double-check locking OK due to 'volatile'.  Just saying...
                //http://en.wikipedia.org/wiki/Double-checked_locking#Usage_in_Java
                if(instance == null)
                    instance = factory.createHelper(context);
            }
        }

        instanceCount++;
        return instance;
    }

    public static void close()
    {

        instanceCount--;
        if(instanceCount == 0)
        {
            synchronized (AndroidSQLiteManager.class)
            {
                if(instance != null)
                {
                    instance.close();
                    instance = null;
                }
            }
        }
        if(instanceCount < 0)
        {
            throw new IllegalStateException("Too many calls to close");
        }
    }

    public interface SQLiteOpenHelperFactory<H extends OrmLiteSQLiteOpenHelper>
    {
        public H createHelper(Context c);
    }
}
