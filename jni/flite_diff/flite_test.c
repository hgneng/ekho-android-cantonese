// gcc -Wall -g -o flite_test flite_test.c -Iinclude build/x86_64-darwin21.6.0/lib/*.a

#include "flite.h"
cst_voice *register_cmu_us_kal(const char *voxdir);

int main(int argc, char **argv)
{
    cst_voice *v;

    if (argc != 2)
    {
        fprintf(stderr,"usage: flite_test FILE\n");
        exit(-1);
    }

    flite_init();

    v = register_cmu_us_kal(NULL);

    // flite_file_to_speech(argv[1],v,"hello.wav");
    cst_wave *flite_wave = flite_text_to_wave(argv[1], v);
    printf("samples: %d", flite_wave->num_samples);
}
