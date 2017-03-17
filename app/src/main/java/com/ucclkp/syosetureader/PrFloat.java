package com.ucclkp.syosetureader;

public class PrFloat
{
    private String mFloat;


    public PrFloat()
    {
        this(null);
    }

    public PrFloat(float value)
    {
        this(String.valueOf(value));
    }

    public PrFloat(String value)
    {
        mFloat = arrange(value);
    }


    private String arrange(String value)
    {
        if (value == null)
            value = "0.0";
        else
        {
            for (int i = 0; i < value.length(); ++i)
            {
                if (value.charAt(i) != '.'
                        && value.charAt(i) > '9'
                        && value.charAt(i) < '0')
                    throw new RuntimeException("The value is not a number!");
            }

            int dot = value.indexOf(".");
            if (dot == -1)
                value += ".0";
            else if (dot == 0)
                value = "0" + value;
            else if (dot == value.length() - 1)
                value += "0";
            else if (dot < value.length() - 2)
                value = value.substring(0, dot + 2);
        }

        int index = 0;
        while (value.charAt(index) == '0' && value.charAt(index + 1) != '.')
            ++index;
        if (index > 0)
            value = value.substring(index);

        return value;
    }

    public void inc()
    {
        StringBuilder builder = new StringBuilder();

        char num = mFloat.charAt(mFloat.length() - 1);
        if (num == '9')
        {
            boolean carry = false;
            for (int i = mFloat.length() - 1; i >= 0; --i)
            {
                char current = mFloat.charAt(i);
                if (current == '.')
                {
                    builder.insert(0, '.');
                    continue;
                }

                if (current == '9')
                {
                    builder.insert(0, '0');
                    carry = true;
                }
                else
                {
                    carry = false;
                    builder.insert(0, (char) ((int) current + 1));
                    if (i > 0)
                        builder.insert(0, mFloat.substring(0, i));
                    break;
                }
            }

            if (carry)
                builder.insert(0, '1');
        }
        else
        {
            builder.append(mFloat.substring(0, mFloat.length() - 1))
                    .append((char) ((int) num + 1));
        }

        mFloat = builder.toString();
    }

    public void dec()
    {
        if (mFloat.equals("0.0"))
            return;

        StringBuilder builder = new StringBuilder();

        char num = mFloat.charAt(mFloat.length() - 1);
        if (num == '0')
        {
            boolean carry = false;
            for (int i = mFloat.length() - 1; i >= 0; --i)
            {
                char current = mFloat.charAt(i);
                if (current == '.')
                {
                    builder.insert(0, '.');
                    continue;
                }

                if (current == '0')
                {
                    builder.insert(0, '9');
                    carry = true;
                }
                else
                {
                    carry = false;
                    builder.insert(0, (char) ((int) current - 1));
                    if (i > 0)
                        builder.insert(0, mFloat.substring(0, i));
                    break;
                }
            }

            if (builder.charAt(0) == '0' && builder.charAt(1) != '.')
                builder.delete(0, 1);
        }
        else
        {
            builder.append(mFloat.substring(0, mFloat.length() - 1))
                    .append((char) ((int) num - 1));
        }

        mFloat = builder.toString();
    }

    public int compareTo(String value)
    {
        return compareTo(new PrFloat(value));
    }

    public int compareTo(PrFloat value)
    {
        if (mFloat.length() > value.mFloat.length())
            return 1;
        else if (mFloat.length() < value.mFloat.length())
            return -1;
        else
        {
            for (int i = 0; i < mFloat.length(); ++i)
            {
                if (mFloat.charAt(i) > value.mFloat.charAt(i))
                    return 1;
                else if (mFloat.charAt(i) < value.mFloat.charAt(i))
                    return -1;
            }
        }

        return 0;
    }

    public float value()
    {
        return Float.valueOf(mFloat);
    }

    public String get()
    {
        return mFloat;
    }

    public boolean isZero()
    {
        return mFloat.equals("0.0");
    }
}
