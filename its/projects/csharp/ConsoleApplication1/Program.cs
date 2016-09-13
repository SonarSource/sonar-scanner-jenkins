using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ConsoleApplication1
{
    public class Program
    {
        public static int Add(int op1, int op2)
        {
            if (op1 == 0)
            {
                return op2;
            }

            if (op2 == 0)
            {
                return op1;
            }

            return op1 + op2;
        }

        static void Main(string[] args)
        {
        }
    }
}
